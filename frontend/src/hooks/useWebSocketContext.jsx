import { useContext } from "react";
import { websocketContext } from "../WebSocketProvider";

const useWebSocketContext = ()=>{
    return useContext(websocketContext)
}

export default useWebSocketContext