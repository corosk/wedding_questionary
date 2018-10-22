package com.entertainment.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ソケット通信時のレスポンス
 *
 * @author t_furuya
 */
public class SocketResponseBean {
    /**
     * レスポンスタイプ
     */
    @JsonProperty
    private String type;

    /**
     * 設定値
     */
    @JsonProperty
    private Map<String, ?> value;

    /**
     * 画面 ID
     */
    private String vid;

    /**
     * レスポンスタイプの取得を行う
     *
     * @return レスポンスタイプ
     */
    public String getType() {
        return type;
    }

    /**
     * レスポンスタイプの設定を行う
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 設定値の取得を行う
     *
     * @return value 設定値
     */
    public Map<String, ?> getValue() {
        return value;
    }

    /**
     * 設定値の設定を行う
     *
     * @param value 設定値
     */
    public void setValue(Map<String, ?> value) {
        this.value = value;
    }

    /**
     * 画面 IDを取得する
     *
     * @return 画面 ID
     */
    public String getVid() {
        return vid;
    }

    /**
     * 画面 IDを設定する
     *
     * @param vid 画面 ID
     */
    public void setVid(String vid) {
        this.vid = vid;
    }
}
