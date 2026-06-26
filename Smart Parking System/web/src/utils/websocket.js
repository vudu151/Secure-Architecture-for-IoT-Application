import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from '../constants';

let stompClient = null;
let reconnectTimeout = null;
const subscriptions = new Map();

export const connectWebSocket = (onConnected, onError) => {
  if (stompClient?.connected) {
    onConnected?.();
    return;
  }

  const token = localStorage.getItem('token');

  stompClient = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    debug: (str) => {
      if (import.meta.env.DEV) {
        console.log('[WS]', str);
      }
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      console.log('[WS] Connected');
      // Re-subscribe after reconnect
      subscriptions.forEach(({ topic, callback }) => {
        const sub = stompClient.subscribe(topic, (message) => {
          try {
            const body = JSON.parse(message.body);
            callback(body);
          } catch {
            callback(message.body);
          }
        });
        subscriptions.set(topic, { topic, callback, subscription: sub });
      });
      onConnected?.();
    },
    onStompError: (frame) => {
      console.error('[WS] STOMP Error:', frame.headers?.message);
      onError?.(frame);
    },
    onWebSocketClose: () => {
      console.log('[WS] Connection closed');
    },
  });

  stompClient.activate();
};

export const disconnectWebSocket = () => {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
  subscriptions.forEach(({ subscription }) => {
    subscription?.unsubscribe?.();
  });
  subscriptions.clear();
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
};

export const subscribeToTopic = (topic, callback) => {
  if (!stompClient?.connected) {
    // Store for re-subscription on reconnect
    subscriptions.set(topic, { topic, callback, subscription: null });
    return null;
  }

  const sub = stompClient.subscribe(topic, (message) => {
    try {
      const body = JSON.parse(message.body);
      callback(body);
    } catch {
      callback(message.body);
    }
  });

  subscriptions.set(topic, { topic, callback, subscription: sub });
  return sub;
};

export const unsubscribeFromTopic = (topic) => {
  const entry = subscriptions.get(topic);
  if (entry?.subscription) {
    entry.subscription.unsubscribe();
  }
  subscriptions.delete(topic);
};

export const sendMessage = (destination, body) => {
  if (stompClient?.connected) {
    stompClient.publish({
      destination,
      body: JSON.stringify(body),
    });
  }
};

export default {
  connect: connectWebSocket,
  disconnect: disconnectWebSocket,
  subscribe: subscribeToTopic,
  unsubscribe: unsubscribeFromTopic,
  send: sendMessage,
};
