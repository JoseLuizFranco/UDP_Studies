import socket
import threading
import tkinter as tk
from tkinter import ttk
import random
import time

UDP_IP = "127.0.0.1"
UDP_PORT = 6000


class ClientManager:
    def __init__(self, master, client_id):
        self.client_id = client_id
        self.sequence_number = 0
        self.auto_send = False
        self.send_interval = 1
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        self.frame = ttk.LabelFrame(master, text=f"Cliente {self.client_id}")
        self.frame.pack(fill="x", padx=5, pady=5)

        self.value_entry = tk.Entry(self.frame)
        self.value_entry.pack(side="left", padx=5, pady=5)

        self.send_button = ttk.Button(self.frame, text="Enviar Manualmente", command=self.send_manual_data)
        self.send_button.pack(side="left", padx=5)

        self.start_button = ttk.Button(self.frame, text="Iniciar Envio Automático", command=self.start_auto_send)
        self.start_button.pack(side="left", padx=5)

        self.stop_button = ttk.Button(self.frame, text="Parar Envio Automático", command=self.stop_auto_send)
        self.stop_button.pack(side="left", padx=5)

        self.interval_label = tk.Label(self.frame, text="Intervalo de Envio (s):")
        self.interval_label.pack(side="left", padx=5)
        self.interval_slider = tk.Scale(self.frame, from_=0.1, to=5.0, resolution=0.1, orient=tk.HORIZONTAL,
                                        command=self.set_interval)
        self.interval_slider.set(self.send_interval)
        self.interval_slider.pack(side="left", padx=5)

    def send_manual_data(self):
        value = self.value_entry.get()
        try:
            message = f"Client {self.client_id}:{self.sequence_number}:{value}"
            self.client_socket.sendto(message.encode(), (UDP_IP, UDP_PORT))
            print(f"Cliente {self.client_id} - Pacote enviado manualmente: {message}")
            self.sequence_number += 1  # Increment sequence number after sending
        except Exception as e:
            print(f"Cliente {self.client_id} - Erro ao enviar pacote: {e}")

    def auto_send_data(self):
        while self.auto_send:
            try:
                value = random.randint(1, 100)
                message = f"Client {self.client_id}:{self.sequence_number}:{value}"
                self.client_socket.sendto(message.encode(), (UDP_IP, UDP_PORT))
                print(f"Cliente {self.client_id} - Pacote enviado automaticamente: {message}")
                self.sequence_number += 1  # Increment sequence number after sending
                time.sleep(self.send_interval)
            except Exception as e:
                print(f"Cliente {self.client_id} - Erro ao enviar pacote automaticamente: {e}")

    def start_auto_send(self):
        if not self.auto_send:
            self.auto_send = True
            self.auto_thread = threading.Thread(target=self.auto_send_data)
            self.auto_thread.daemon = True
            self.auto_thread.start()

    def stop_auto_send(self):
        self.auto_send = False

    def set_interval(self, value):
        try:
            self.send_interval = float(value)
            print(f"Cliente {self.client_id} - Intervalo de envio ajustado para: {self.send_interval} segundos")
        except ValueError:
            print("Valor de intervalo inválido")


class App:
    def __init__(self, root):
        self.root = root
        self.root.title("Gerenciador de Clientes UDP")
        self.clients = []

        self.add_client_button = ttk.Button(root, text="Adicionar Cliente", command=self.add_client)
        self.add_client_button.pack(pady=10)

    def add_client(self):
        client_id = len(self.clients) + 1
        client_manager = ClientManager(self.root, client_id)
        self.clients.append(client_manager)


if __name__ == "__main__":
    root = tk.Tk()
    app = App(root)
    root.mainloop()
