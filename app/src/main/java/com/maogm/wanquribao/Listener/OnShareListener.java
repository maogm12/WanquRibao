package com.maogm.wanquribao.Listener;

/**
 *
 */
public interface OnShareListener {
    void onShareText(String subject, String body);
    void onGlobalShareContentChanged(String subject, String body);
    void onRestoreGlobalShare();
    void onGlobalShareEnabled(boolean enable);
}