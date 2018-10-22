package com.entertainment.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ソケット通信時のリクエストパラメータ
 *
 * @author t_furuya
 */
public class SocketRequestBean {
    /**
     * リクエストタイプ名
     */
    @JsonProperty(value = "type", required = true)
    private String requestTypeName;

    /**
     * リクエスト値
     */
    @JsonProperty
    private String value;

    /**
     * 画面 ID
     */
    @JsonProperty
    private String vid;

    /**
     * リクエストタイプ名を取得する
     *
     * @return リクエストタイプ名
     */
    public String getRequestTypeName() {
        return requestTypeName;
    }

    /**
     * リクエストタイプ名を設定する
     *
     * @param requestTypeName リクエストタイプ名
     */
    public void setRequestTypeName(String requestTypeName) {
        this.requestTypeName = requestTypeName;
    }

    /**
     * リクエスト値を取得する
     *
     * @return リクエスト値
     */
    public String getValue() {
        return value;
    }

    /**
     * リクエスト値を設定する
     *
     * @param value リクエスト値
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * リクエストタイプを取得する。
     *
     * @return リクエストタイプ
     */
    public RequestType getType() {
        return RequestType.getType(requestTypeName);
    }

    /**
     * 画面 ID を取得する。
     *
     * @return 画面 ID
     */
    public String getVid() {
        return vid;
    }

    /**
     * 画面 ID の設定をする。
     *
     * @param vid 画面 ID
     */
    public void setVid(String vid) {
        this.vid = vid;
    }

    /**
     * リクエストタイプ
     *
     * @author t_furuya
     */
    public enum RequestType {
        CONNECT("connect"),
        DIS_CONNECT("disConnect"),
        ANSWER("answer"),
        FORCE_CLEAR("clear"),
        START("start"),
        STOP("stop"),
        SYNC("sync"),
        UNKNOWN("un");

        /**
         * リクエストタイプ名
         */
        private String requestTypeName;

        /**
         * コンストラクタ
         *
         * @param requestTypeName リクエストタイプ名
         */
        private RequestType(String requestTypeName) {
            this.requestTypeName = requestTypeName;
        }

        /**
         * リクエストタイプ名に紐づくリクエストタイプを取得する。<br>
         * 未知のリクエストタイプが指定された場合、{@code RequestType#UNKNOWN} を返却する。
         *
         * @param requestTypeName リクエストタイプ名
         * @return リクエストタイプ
         */
        public static RequestType getType(String requestTypeName) {
            return Arrays.asList(RequestType.values()).stream().filter(type -> {
                return type.requestTypeName.equals(requestTypeName);
            }).findFirst().orElse(UNKNOWN);
        }

        /**
         * 対応した文字列を取得する
         */
        public String toString() {
            return this.requestTypeName;
        }
    }
}
