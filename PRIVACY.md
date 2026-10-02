# Privacy

AionUi Remote Companion does not include analytics, advertising, or an account system. The host name or IP address is stored locally on the device using Android app preferences. WebView cookies and local site storage are kept by Android WebView so the AionUi login can persist.

The app connects only to the private host entered by the user, on AionUi's configured port 25808. HTTP connections are restricted to private LAN/Tailscale destinations and the selected AionUi origin. HTTPS links outside the AionUi origin open in the device's browser. Files selected in AionUi are sent to the AionUi host; downloads initiated by AionUi are saved to the device's Downloads folder.

The app developer does not receive the host address, credentials, prompts, conversations, or files. The AionUi host and the configured AI/model provider process the data according to their own settings and policies. Tailscale encrypts traffic when the devices communicate over the Tailscale network. HTTP over a regular LAN does not have an additional TLS layer.
