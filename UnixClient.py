import os
import sys
import time
import socket
import logging
import ssl

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

SOCKFILE = "/tmp/snort_alert"
BUFSIZE = 1024


# Must to set your controller IP here
CONTROLLER_IP = '127.0.0.1'

# Controller port is 51234 by default.
# If you want to change the port number
# you need to set the same port number in the controller application.
CONTROLLER_PORT = 51234

# TODO: TLS/SSL wrapper for socket


class SnortListener():

    def __init__(self):
        self.unsock = None
        self.so = None

    def start_send(self):
        if ssl.HAS_TLSv1_3:
            print("{0} with support for TLS 1.3"
		          .format(ssl.OPENSSL_VERSION))
        '''Open a client on Network Socket'''
        CERTFILE = os.path.join(os.path.dirname(__file__), "certificate.pem")
        context = context = ssl.SSLContext(ssl.PROTOCOL_TLS)
        context.load_cert_chain(CERTFILE)
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        context.options |= ssl.OP_NO_TLSv1 | ssl.OP_NO_TLSv1_1 | ssl.OP_NO_SSLv2 | ssl.OP_NO_SSLv3
        self.so = context.wrap_socket(s)
        self.so.connect((CONTROLLER_IP, CONTROLLER_PORT))
        print("entrou")

    def start_recv(self):
        '''Open a server on Unix Domain Socket'''
        if os.path.exists(SOCKFILE):
            os.unlink(SOCKFILE)

        self.unsock = socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM)
        self.unsock.bind(SOCKFILE)
        logger.info("Unix Domain Socket listening...")
        self.recv_loop()

    def recv_loop(self):
        '''Receive Snort alert on Unix Domain Socket and
        send to Network Socket Server forever'''
        logger.info("Start the network socket client....")
        self.start_send()
        while True:
            data = self.unsock.recv(BUFSIZE)
            if data:
                logger.debug("Send {0} bytes of data.".format(sys.getsizeof(data)))
                # data == 65900 byte
                self.tcp_send(data)
            else:
                pass

    def tcp_send(self, data):
        self.so.sendall(data + '\n')
        logger.info("Send the alert messages to Floodlight.")
        logger.info(data)


if __name__ == '__main__':
    server = SnortListener()
    server.start_recv()
