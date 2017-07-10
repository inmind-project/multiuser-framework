# GLOBALS

C_CLIENT = b"MDPC01"  # bytes("MDPC01", 'UTF-8')
S_WORKER = b"MDPW01"  # bytes("MDPW01", 'UTF-8')
S_READY = bytes(1)
S_REQUEST = bytes(2)
S_REPLY = bytes(3)
S_HEARTBEAT = bytes(4)
S_DISCONNECT = bytes(5)

verbose = False
stop = False
port = "5590"  # this is our dialogue server port

