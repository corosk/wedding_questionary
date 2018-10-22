package com.entertainment.handler;

import static com.entertainment.model.SocketRequestBean.RequestType.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.entertainment.model.SessionInformationBean;
import com.entertainment.model.SocketRequestBean;
import com.entertainment.model.SocketRequestBean.RequestType;
import com.entertainment.model.SocketResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ソケット通信のハンドラ
 *
 * @author t_furuya
 */
@Component
public class SocketHandler extends TextWebSocketHandler {
    /**
     * logger
     */
    private static final Logger logger = Logger.getLogger(SocketHandler.class);

    /**
     * 画面 ID(管理画面)
     */
    private static final String VIEW_ID_MANAGEMENT = "00";

    /**
     * 画面 ID(表示画面)
     */
    private static final String VIEW_ID_RESULT_VIEW = "10";

    /**
     * 画面 ID(ボタン画面)
     */
    private static final String VIEW_ID_BUTTON = "20";

    /**
     * 一時セッションプール
     */
    private Map<String, WebSocketSession> temporarySessionPool = Collections.synchronizedMap(new ConcurrentHashMap<>());

    /**
     * セッションプール (ボタン画面用)
     */
    private Map<String, SessionInformationBean> userSessionPool = Collections
            .synchronizedMap(new ConcurrentHashMap<>());

    /**
     * セッションプール (管理画面用)
     */
    private Map<String, WebSocketSession> managerSessionPool = Collections.synchronizedMap(new ConcurrentHashMap<>());

    /**
     * セッションプール（表示画面用）
     */
    private Map<String, WebSocketSession> resultViewSessionPool = Collections
            .synchronizedMap(new ConcurrentHashMap<>());

    /**
     * 回答ロック状況
     */
    private boolean isLocked;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        temporarySessionPool.put(session.getId(), session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        temporarySessionPool.remove(session.getId());

        String vid = "99";

        if (managerSessionPool.containsKey(session.getId())) {
            vid = VIEW_ID_MANAGEMENT;
            managerSessionPool.remove(session.getId());
        }

        if (resultViewSessionPool.containsKey(session.getId())) {
            vid = VIEW_ID_RESULT_VIEW;
            resultViewSessionPool.remove(session.getId());
        }

        if (userSessionPool.containsKey(session.getId())) {
            vid = VIEW_ID_BUTTON;
            userSessionPool.remove(session.getId());
        }

        Map<String, Object> resultValues = new HashMap<>();
        resultValues.put("sessionId", session.getId());
        resultValues.put("connectionCount", userSessionPool.size());
        String response = generateResponseJson(DIS_CONNECT.toString(), vid, resultValues);

        // 削除対象となったセッション ID を管理画面に通知する。
        managerSessionPool.keySet().forEach(sessionId -> {
            try {
                managerSessionPool.get(sessionId).sendMessage(new TextMessage(response));
            } catch (Exception e) {
                managerSessionPool.remove(sessionId);
                logger.error("error is occured", e);
            }
        });

        // 削除対象となったセッション ID を管理画面に通知する。
        resultViewSessionPool.keySet().forEach(sessionId -> {
            try {
                resultViewSessionPool.get(sessionId).sendMessage(new TextMessage(response));
            } catch (Exception e) {
                logger.error("error is occured", e);
                resultViewSessionPool.remove(sessionId);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        synchronized (this) {
            ObjectMapper mapper = new ObjectMapper();
            SocketRequestBean socketRequest = null;
            try {
                socketRequest = mapper.readValue(message.getPayload(), SocketRequestBean.class);
                RequestType type = socketRequest.getType();

                switch (type) {
                    case CONNECT:
                        doConnectFunction(session, socketRequest);
                        break;
                    case ANSWER:
                        doAnswerFunction(session, socketRequest);
                        break;
                    case FORCE_CLEAR:
                        doForceClear(session);
                        break;
                    case START:
                        doStartFunction();
                        break;
                    case STOP:
                        doStopFunction();
                        break;
                    case SYNC:
                        doSyncFunction(session, socketRequest);
                        break;
                    case UNKNOWN:
                    default:
                        break;
                }

            } catch (Exception e) {
                logger.error("Error is Occured", e);
            }
        }

    }

    /**
     * コマンド：connectの処理を行う
     *
     * @param session       セッション
     * @param socketRequest リクエスト情報
     */
    private void doConnectFunction(WebSocketSession session, SocketRequestBean socketRequest) {
        allocateSessionPool(session.getId(), socketRequest.getVid());

        Map<String, Object> resultValues = new HashMap<>();
        resultValues.put("sessionId", session.getId());
        resultValues.put("connectionCount", userSessionPool.size());

        String response = generateResponseJson(CONNECT.toString(), socketRequest.getVid(), resultValues);

        try {
            session.sendMessage(new TextMessage(response));
        } catch (IOException e) {
            logger.error("Error is Occured", e);
        }


        // 管理ページへ接続ユーザの情報を返却する処理
        managerSessionPool.keySet().stream().forEach(key -> {
            try {
                managerSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                managerSessionPool.remove(key);
            }
        });

        // 結果表示ページへ接続ユーザの情報を返却する処理
        resultViewSessionPool.keySet().stream().forEach(key -> {
            try {
                resultViewSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                resultViewSessionPool.remove(key);
            }
        });
    }

    /**
     * コマンド：answerの処理を行う
     *
     * @param session       セッション
     * @param socketRequest リクエスト情報
     */
    private void doAnswerFunction(WebSocketSession session, SocketRequestBean socketRequest) {
        // ロック中は設定の反映を行わないで処理を終了する
        if (isLocked) {
            return;
        }

        Boolean answer = setAnswer(session, socketRequest.getValue());
        Map<String, Object> resultValues = new HashMap<>();
        resultValues.put("sessionId", session.getId());
        resultValues.put("answer", answer);

        String response = generateResponseJson(ANSWER.toString(), VIEW_ID_BUTTON, resultValues);

        try {
            session.sendMessage(new TextMessage(response));
        } catch (IOException e) {
            logger.error("Error is Occured", e);
            userSessionPool.remove(socketRequest.getValue());
        }


        managerSessionPool.keySet().stream().forEach(key -> {
            try {
                managerSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                managerSessionPool.remove(key);
            }
        });
    }

    /**
     * コマンド：forceClearの処理を行う
     *
     * @param session セッション
     */
    private void doForceClear(WebSocketSession session) {
        isLocked = false;

        userSessionPool.keySet().stream().forEach(key -> {
            SessionInformationBean sessionInfo = userSessionPool.get(key);
            sessionInfo.setAnswer(false);

            Map<String, String> resultValues = new HashMap<>();
            resultValues.put("sessionId", key);
            resultValues.put("answer", "false");

            String response = generateResponseJson(FORCE_CLEAR.toString(), VIEW_ID_MANAGEMENT, resultValues);
            try {
                sessionInfo.getSession().sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                userSessionPool.remove(key);
            }
        });

        // 管理画面と結果表示画面での処理は共通のため同様の処理を行う
        Arrays.asList(managerSessionPool, resultViewSessionPool).forEach(pool -> {
            pool.keySet().forEach(key -> {
                Map<String, String> resultValues = new HashMap<>();
                resultValues.put("sessionId", key);
                resultValues.put("answer", "false");

                String response = generateResponseJson(FORCE_CLEAR.toString(), VIEW_ID_MANAGEMENT, resultValues);
                try {
                    pool.get(key).sendMessage(new TextMessage(response));
                } catch (IOException e) {
                    logger.info("Error is Occured", e);
                    pool.remove(key);
                }
            });
        });
    }

    /**
     * コマンド：startの処理を行う
     */
    private void doStartFunction() {
        String response = generateResponseJson(START.toString(), VIEW_ID_MANAGEMENT, null);

        userSessionPool.keySet().stream().forEach(key -> {
            try {
                userSessionPool.get(key).getSession().sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                userSessionPool.remove(key);
            }
        });

        managerSessionPool.keySet().stream().forEach(key -> {
            try {
                managerSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                managerSessionPool.remove(key);
            }
        });

        resultViewSessionPool.keySet().stream().forEach(key -> {
            try {
                resultViewSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                resultViewSessionPool.remove(key);
            }
        });
    }

    /**
     * コマンド：stop の処理を行う
     */
    private void doStopFunction() {
        isLocked = true;

        // 回答が true のユーザの集計
        Long result = userSessionPool.keySet().stream().filter(key -> {
            SessionInformationBean sessionInfo = userSessionPool.get(key);

            return sessionInfo.getAnswer();
        }).count();

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("result", String.format("%d", result));
        resultMap.put("connectionCount", String.format("%d", userSessionPool.size()));
        String response = generateResponseJson(STOP.toString(), VIEW_ID_MANAGEMENT, resultMap);

        userSessionPool.keySet().stream().forEach(key -> {
            try {
                userSessionPool.get(key).getSession().sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                userSessionPool.remove(key);
            }
        });

        managerSessionPool.keySet().stream().forEach(key -> {
            try {
                managerSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                managerSessionPool.remove(key);
            }
        });

        resultViewSessionPool.keySet().stream().forEach(key -> {
            try {
                resultViewSessionPool.get(key).sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.info("Error is Occured", e);
                resultViewSessionPool.remove(key);
            }
        });
    }

    /**
     * コマンド:syncの処理を行う
     *
     * @param session       セッション
     * @param socketRequest リクエスト
     */
    private void doSyncFunction(WebSocketSession session, SocketRequestBean socketRequest) {
        List<Map<String, Object>> connectionList = userSessionPool.keySet().stream().map(key -> {
            Map<String, Object> connectionInfo = new HashMap<>();

            connectionInfo.put("sessionId", key);
            SessionInformationBean sessionInfo = userSessionPool.get(key);
            connectionInfo.put("answer", sessionInfo.getAnswer());

            return connectionInfo;
        }).collect(Collectors.toList());

        int connectedUserCnt = userSessionPool.size();

        Map<String, Object> resultValues = new HashMap<>();
        resultValues.put("connectionList", connectionList);
        resultValues.put("connectionCount", connectedUserCnt);
        String response = generateResponseJson(SYNC.toString(), socketRequest.getVid(), resultValues);

        try {
            session.sendMessage(new TextMessage(response));
        } catch (IOException e) {
            logger.error("Error is Occured", e);
            userSessionPool.get(session.getId());
        }

    }

    /**
     * レスポンスJsonを生成する
     *
     * @param type  レスポンスタイプ
     * @param vid   画面 ID
     * @param value 設定値
     * @return レスポンスJSON文字列
     */
    private String generateResponseJson(String type, String vid, Map<String, ?> value) {
        String result = null;

        try {
            SocketResponseBean response = new SocketResponseBean();
            response.setType(type);
            response.setVid(vid);
            response.setValue(value);

            ObjectMapper mapper = new ObjectMapper();
            result = mapper.writeValueAsString(response);
        } catch (IOException e) {
            throw new RuntimeException();
        }

        return result;
    }

    /**
     * On/Offリクエスト時の処理<br>
     * 回答内容の反映を行う。
     *
     * @param session
     * @param message
     * @return ON/OFFを示す文字列
     */
    private Boolean setAnswer(WebSocketSession session, String currentAnswer) {
        if (!isBooleanString(currentAnswer)) {
            throw new RuntimeException("Unknown message was specified.");
        }

        Boolean nextAnswer = !(new Boolean(currentAnswer));

        SessionInformationBean sessionInfo = userSessionPool.get(session.getId());
        sessionInfo.setAnswer(nextAnswer);

        return nextAnswer;
    }

    /**
     * セッションプールの振り分けを行う
     *
     * @param 画面 ID
     */
    private void allocateSessionPool(String sessionId, String vid) {
        // すでに振り分けが完了している場合は何もしない
        if (!temporarySessionPool.containsKey(sessionId)) {
            return;
        }

        WebSocketSession session = temporarySessionPool.get(sessionId);
        temporarySessionPool.remove(sessionId);

        // 管理画面からのアクセス
        if (VIEW_ID_MANAGEMENT.equals(vid)) {
            managerSessionPool.put(sessionId, session);
            return;
        }

        if (VIEW_ID_RESULT_VIEW.equals(vid)) {
            resultViewSessionPool.put(sessionId, session);
            return;
        }

        SessionInformationBean connInfo = new SessionInformationBean();
        connInfo.setSession(session);
        connInfo.setAnswer(false);

        userSessionPool.put(sessionId, connInfo);
    }

    /**
     * 指定された文字列が true/false のどちらかであるかを判定する
     *
     * @param string 判定文字列
     * @return booleanを示す文字列の場合 {@code true}
     */
    private boolean isBooleanString(String string) {
        return "true".equalsIgnoreCase(string) || "false".equalsIgnoreCase(string);
    }
}
