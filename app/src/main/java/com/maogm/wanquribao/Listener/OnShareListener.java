package com.maogm.wanquribao.Listener;

/**
 *
 */
public interface OnShareListener {
    void onShareText(String subject, String body);
    void onGlobalShareChanged(String subject, String body);
    void onRestoreGlobalShare();
}