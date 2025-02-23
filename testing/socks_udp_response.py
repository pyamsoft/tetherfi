#!/usr/bin/python3
# socks_udp_response.py - simple tool to test UDP support of SOCKS5 proxy.
#
# udpchk.py - simple tool to test UDP support of SOCKS5 proxy.
#
# Copyright (C) 2025 pyamsoft
# Copyright (C) 2016-2025 Zhuofei Wang <semigodking@gmail.com>
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License.

from __future__ import print_function
import sys
if sys.platform == 'win32' and (sys.version_info.major < 3
                                or (sys.version_info.major == 3 and sys.version_info.minor < 4)):
    # inet_pton is only supported on Windows since Python 3.4
    import win_inet_pton
import socket
import socks
from traceback import print_exc

def test_udp(addr, port, user=None, pwd=None):
    s = socks.socksocket(socket.AF_INET, socket.SOCK_DGRAM) # Same API as socket.socket in the standard lib
    try:
        rdns=True
        remote_host="dns.google"
        remote_port=53

        s.set_proxy(socks.SOCKS5, addr, port, rdns , user, pwd) # SOCKS4 and SOCKS5 use port 1080 by default
        # Can be treated identical to a regular socket object
        # Raw DNS request
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
        s.sendto(bytes(req), (remote_host,  remote_port))
        (rsp, address)= s.recvfrom(4096)
        if rsp[0] == req[0] and rsp[1] == req[1]:
            print("UDP check passed")
            return 0
        else:
            print("Invalid response")
            return 1
    except socket.error as e:
        print_exc()
        print(repr(e))
    except socks.ProxyError as e:
        print_exc()
        print(e.msg)


def main():
    returned = test_udp("192.168.49.1", 8229)
    sys.exit(returned)

if __name__ == "__main__":
    main()
