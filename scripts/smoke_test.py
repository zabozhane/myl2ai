#!/usr/bin/env python3
"""Protocol-level smoke test for the login and game servers (stdlib only).

Checks what the C6 client does first on each port:
  * login (2106): the server must greet a new connection with an Init packet;
  * game (7777): ProtocolVersion 746 must be answered with KeyPacket, an unknown
    protocol must get the connection closed.

Usage: smoke_test.py [login_host] [game_host]   (both default to 127.0.0.1)
"""
import socket
import struct
import sys

LOGIN_PORT = 2106
GAME_PORT = 7777
CLIENT_PROTOCOL = 746
UNSUPPORTED_PROTOCOL = 999
PROTOCOL_VERSION_OPCODE = 0x00
KEY_PACKET_OPCODE = 0x00
KEY_PACKET_ACCEPTED = 0x01
PACKET_HEADER_SIZE = 2  # little-endian uint16, includes itself
TIMEOUT_SECONDS = 10


def read_exact(connection, size):
    data = b""
    while len(data) < size:
        chunk = connection.recv(size - len(data))
        if not chunk:
            raise ConnectionError(f"connection closed after {len(data)} of {size} bytes")
        data += chunk
    return data


def read_packet(connection):
    (packet_size,) = struct.unpack("<H", read_exact(connection, PACKET_HEADER_SIZE))
    return read_exact(connection, packet_size - PACKET_HEADER_SIZE)


def send_packet(connection, body):
    connection.sendall(struct.pack("<H", len(body) + PACKET_HEADER_SIZE) + body)


def connect(host, port):
    return socket.create_connection((host, port), timeout=TIMEOUT_SECONDS)


def check_login_greets(host):
    # The Init body is Blowfish-encrypted, so only its framing is checked here.
    with connect(host, LOGIN_PORT) as connection:
        body = read_packet(connection)
    if not body:
        raise AssertionError("login: empty Init packet")
    return f"login {host}:{LOGIN_PORT} sent Init ({len(body)} bytes)"


def request_protocol(host, protocol):
    connection = connect(host, GAME_PORT)
    send_packet(connection, struct.pack("<Bi", PROTOCOL_VERSION_OPCODE, protocol))
    return connection


def check_game_accepts_client_protocol(host):
    with request_protocol(host, CLIENT_PROTOCOL) as connection:
        body = read_packet(connection)
    if body[:2] != bytes([KEY_PACKET_OPCODE, KEY_PACKET_ACCEPTED]):
        raise AssertionError(f"game: protocol {CLIENT_PROTOCOL} not accepted, got {body[:2].hex()}")
    return f"game {host}:{GAME_PORT} accepted protocol {CLIENT_PROTOCOL}"


def check_game_rejects_unknown_protocol(host):
    with request_protocol(host, UNSUPPORTED_PROTOCOL) as connection:
        try:
            body = read_packet(connection)
        except ConnectionError:
            return f"game {host}:{GAME_PORT} rejected protocol {UNSUPPORTED_PROTOCOL}"
    raise AssertionError(f"game: protocol {UNSUPPORTED_PROTOCOL} unexpectedly answered with {body.hex()}")


def main():
    login_host = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
    game_host = sys.argv[2] if len(sys.argv) > 2 else "127.0.0.1"
    checks = [
        lambda: check_login_greets(login_host),
        lambda: check_game_accepts_client_protocol(game_host),
        lambda: check_game_rejects_unknown_protocol(game_host),
    ]
    failed = False
    for check in checks:
        try:
            print("OK  ", check())
        except (OSError, AssertionError) as error:
            failed = True
            print("FAIL", error)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
