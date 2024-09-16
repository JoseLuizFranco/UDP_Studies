import socket
import threading
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import tkinter as tk
from collections import OrderedDict
import itertools

UDP_IP = "0.0.0.0"
UDP_PORT = 5005

client_expected_sequence = {}
client_lost_packets = {}
client_data_values = OrderedDict()
client_data_count = {}
client_colors = {}
colors = itertools.cycle(plt.rcParams['axes.prop_cycle'].by_key()['color'])


def udp_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket.bind((UDP_IP, UDP_PORT))
    print(f"Servidor UDP escutando na porta {UDP_PORT}...")

    while True:
        data, addr = server_socket.recvfrom(1024)
        data_str = data.decode(errors='ignore').strip()
        print(f"Recebido de {addr[0]}: {data_str}")
        try:
            parts = data_str.split(":")
            if len(parts) >= 3:
                client_id = parts[0].strip()
                sequence_number = int(parts[1].strip())
                value = int(parts[2].strip())

                expected_sequence = client_expected_sequence.get(client_id, 0)
                lost_packets = client_lost_packets.get(client_id, 0)

                if sequence_number > expected_sequence:
                    missing_packets = sequence_number - expected_sequence
                    lost_packets += missing_packets
                    client_lost_packets[client_id] = lost_packets
                    print(f"{client_id}: Pacotes perdidos detectados: {missing_packets}")

                client_expected_sequence[client_id] = sequence_number + 1

                client_data_count[client_id] = client_data_count.get(client_id, 0) + 1

                if client_id not in client_data_values:
                    client_data_values[client_id] = []
                    client_colors[client_id] = next(colors)
                client_data_values[client_id].append((sequence_number, value))

                print(f"Valor registrado: {value} de {client_id} (Seq: {sequence_number})")

            else:
                print(f"Formato de mensagem inválido: {data_str}")

        except ValueError:
            print(f"Erro ao decodificar o pacote recebido de {addr[0]}: {data_str}")


def update_plots():
    ax1.clear()
    for client_id, values in client_data_values.items():
        sequences, data_values = zip(*values)
        ax1.plot(sequences, data_values, label=client_id, color=client_colors[client_id])
    ax1.set_title("Valores Recebidos dos Clientes")
    ax1.set_xlabel("Número de Sequência")
    ax1.set_ylabel("Valor do Dado")
    ax1.legend()

    ax2.clear()
    clients = list(client_data_count.keys())
    counts = [client_data_count[client] for client in clients]
    colors_list = [client_colors[client] for client in clients]

    ax2.bar(clients, counts, label="Pacotes Recebidos por Cliente", color=colors_list)
    ax2.set_title("Quantidade de Pacotes Recebidos por Cliente")
    ax2.set_xlabel("Cliente")
    ax2.set_ylabel("Quantidade de Pacotes")
    ax2.legend()

    for i, (client, count) in enumerate(zip(clients, counts)):
        ax2.text(i, count + 0.5, str(count), color='blue', fontweight='bold', ha='center')

    ax3.clear()
    clients_lost = list(client_lost_packets.keys())
    lost_counts = [client_lost_packets[client] for client in clients_lost]
    colors_lost = [client_colors[client] for client in clients_lost]

    ax3.bar(clients_lost, lost_counts, label="Pacotes Perdidos por Cliente", color=colors_lost)
    ax3.set_title("Quantidade de Pacotes Perdidos por Cliente")
    ax3.set_xlabel("Cliente")
    ax3.set_ylabel("Quantidade de Pacotes Perdidos")
    ax3.legend()

    for i, (client, count) in enumerate(zip(clients_lost, lost_counts)):
        ax3.text(i, count + 0.5, str(count), color='red', fontweight='bold', ha='center')

    fig.tight_layout()
    canvas.draw()
    root.after(1000, update_plots)


root = tk.Tk()
root.title("Dashboard de Pacotes UDP")

fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(14, 9))
canvas = FigureCanvasTkAgg(fig, master=root)
canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)

server_thread = threading.Thread(target=udp_server)
server_thread.daemon = True
server_thread.start()

update_plots()

root.mainloop()
