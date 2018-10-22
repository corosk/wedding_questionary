package com.entertainment.model;

import org.springframework.web.socket.WebSocketSession;

/**
 * コネクション情報
 *
 * @author t_furuya
 */
public class SessionInformationBean {
    /**
     * セッション情報
     */
    private WebSocketSession session;

    /**
     * 回答状況
     */
    private Boolean answer;

    /**
     * セッション情報の取得を行う
     *
     * @return セッション
     */
    public WebSocketSession getSession() {
        return session;
    }

    /**
     * セッションの設定を行う
     *
     * @param セッション
     */
    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    /**
     * 回答状況の取得を行う
     *
     * @return 回答状況
     */
    public Boolean getAnswer() {
        return answer;
    }

    /**
     * 回答状況の設定を行う
     *
     * @param 回答状況
     */
    public void setAnswer(Boolean answer) {
        this.answer = answer;
    }
}
