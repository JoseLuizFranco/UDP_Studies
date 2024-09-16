import socket
import threading
import random

PROXY_HOST = '127.0.0.1'
CLIENT_PORT = 6000
SERVER_PORT = 5005
PROXY_TO_SERVER_PORT = 6001
PACKET_LOSS_PROBABILITY = 0.2


def client_to_server():
    proxy_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    proxy_socket.bind((PROXY_HOST, CLIENT_PORT))
    print(f"Proxy listening on {PROXY_HOST}:{CLIENT_PORT} for client packets")

    while True:
        data, addr = proxy_socket.recvfrom(65535)
        if random.random() >= PACKET_LOSS_PROBABILITY:
            proxy_socket.sendto(data, (PROXY_HOST, SERVER_PORT))
            print(f"Forwarded packet to server: {data.decode().strip()}")
        else:
            print(f"Dropped packet from client: {data.decode().strip()}")


def server_to_client():
    proxy_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    proxy_socket.bind((PROXY_HOST, PROXY_TO_SERVER_PORT))
    print(f"Proxy listening on {PROXY_HOST}:{PROXY_TO_SERVER_PORT} for server packets")

    while True:
        data, addr = proxy_socket.recvfrom(65535)
        if random.random() >= PACKET_LOSS_PROBABILITY:
            proxy_socket.sendto(data, (PROXY_HOST, addr[1]))
            print(f"Forwarded packet to client: {data.decode().strip()}")
        else:
            print(f"Dropped packet from server: {data.decode().strip()}")


if __name__ == "__main__":
    threading.Thread(target=client_to_server, daemon=True).start()
    threading.Thread(target=server_to_client, daemon=True).start()
    print("UDP Proxy started. Press Ctrl+C to stop.")
    try:
        while True:
            pass
    except KeyboardInterrupt:
        print("UDP Proxy stopped.")
