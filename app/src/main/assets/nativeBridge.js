// src/services/nativeBridge.js

export const NativeBridge = {
  isAvailable() {
    return typeof window.AndroidBridge !== 'undefined';
  },

  async openApp(appName) {
    const cleanApp = appName.toLowerCase().trim();
    if (this.isAvailable()) {
      return window.AndroidBridge.openApp(cleanApp);
    }
    
    // Web Fallbacks
    const deepLinks = {
      whatsapp: 'whatsapp://',
      youtube: 'https://youtube.com',
      instagram: 'https://instagram.com',
      chrome: 'https://google.com'
    };

    if (deepLinks[cleanApp]) {
      window.location.href = deepLinks[cleanApp];
      return JSON.stringify({ status: "success", method: "web_fallback" });
    }
    return JSON.stringify({ status: "failed", reason: "Native bridge unavailable for this app." });
  },

  async makeCall(phoneNumber) {
    const cleanNumber = phoneNumber.replace(/[^0-9+]/g, '');
    if (this.isAvailable()) {
      return window.AndroidBridge.makeCall(cleanNumber);
    }
    window.location.href = `tel:${cleanNumber}`;
    return JSON.stringify({ status: "success", method: "tel_link" });
  },

  async callContact(contactName) {
    if (this.isAvailable()) {
      const response = window.AndroidBridge.callContact(contactName);
      return response; // Returns JSON string: { status: "matched|multiple|not_found", data: [...] }
    }
    return JSON.stringify({ status: "failed", reason: "Browser cannot access native contacts." });
  }
};
