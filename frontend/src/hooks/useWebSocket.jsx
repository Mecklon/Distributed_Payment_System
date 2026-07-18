import { useMemo ,useEffect} from "react";
import { Client } from "@stomp/stompjs";

const brokerURL = import.meta.env.VITE_USE_NGINX === "true"
  ? `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`
  : "ws://localhost:9092/ws";

export function useWebSocket(token) {
  const client = useMemo(() => {
    return new Client({
      brokerURL,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: {
        Authorization: `Bearer ${token}`, 
      },
      reconnectDelay: 5000, 
    });
  }, [token]);

  useEffect(() => {
    client.activate();  

    return () => {
      client.deactivate(); 
    };
  }, [client]);

  return client;
}
