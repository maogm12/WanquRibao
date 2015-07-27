package com.maogm.wanquribao.Listener;

/**
 * Interface to open webview
 * @author Guangming Mao
 */
public interface WebViewManager {
    void openUrl(String url, String subject, String body);
    void openHtml(String html, String subject, String body);
}