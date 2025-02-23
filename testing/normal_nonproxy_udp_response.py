#!/usr/bin/python3

import socket

req = [
    # Transaction ID
    0x12, 0x34,
    # Flags: This is a DNS Request
    0x00, 0x01,
    # Questions: There is 1 question
    0x00, 0x01,
    # Answers: There are NO answers yet
    0x00, 0x00,
    # Authority RR: none in request
    0x00, 0x00,
    # Additional RR: none in request
    0x00, 0x00,
    # Question section
    # first byte is length so:
    # (5) baidu
    # (3) com
    # end with 00
    0x05, 0x62, 0x61, 0x69, 0x64, 0x75, 0x03, 0x63, 0x6f, 0x6d, 0x00,
    # Type: A record
    0x00, 0x01,
    # Class: Internet
    0x00, 0x01
]

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
remote_host = "dns.google"
remote_port = 53
s.sendto(bytes(req), (remote_host,  remote_port))
(rsp, address)= s.recvfrom(4096)
if rsp[0] == req[0] and rsp[1] == req[1]:
    print("UDP check passed", rsp)
    #print(rsp)
else:
    print("Invalid response", rsp)
