public enum State {
    INITIAL,
    SENT_PING,
    SENT_REPLCONF_PORT, 
    SENT_REPLCONF_CAPA, 
    SENT_PSYNC, 
    COMPLETE
}
