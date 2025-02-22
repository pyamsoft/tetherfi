#!/usr/bin/python3
# udpchk.py - simple tool to test UDP support of SOCKS5 proxy.
# Copyright (C) 2016-2017 Zhuofei Wang <semigodking@gmail.com>
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

def test_udp(typ, addr, port, user=None, pwd=None):
    s = socks.socksocket(socket.AF_INET, socket.SOCK_DGRAM) # Same API as socket.socket in the standard lib
    try:
        rdns=True
        remote_host="dns.google"
        remote_port=53

        s.set_proxy(socks.SOCKS5, addr, port, rdns , user, pwd) # SOCKS4 and SOCKS5 use port 1080 by default
        # Can be treated identical to a regular socket object
        # Raw DNS request
        req = b"\x12\x34\x01\x00\x00\x01\x00\x00\x00\x00\x00\x00\x05\x62\x61\x69\x64\x75\x03\x63\x6f\x6d\x00\x00\x01\x00\x01"
        s.sendto(req, (remote_host,  remote_port))
        (rsp, address)= s.recvfrom(4096)
        if rsp[0] == req[0] and rsp[1] == req[1]:
            print("UDP check passed")
            #print(rsp)
        else:
            print("Invalid response")
    except socket.error as e:
        print_exc()
        print(repr(e))
    except socks.ProxyError as e:
        print_exc()
        print(e.msg)


def main():
    import os
    import argparse
    def ip_port(string):
        value = int(string)
        if value <= 0 or value > 65535:
            msg = "%r is not valid port number" % string
            raise argparse.ArgumentTypeError(msg)
        return value

    parser = argparse.ArgumentParser(prog=os.path.basename(__file__), 
        description='Test SOCKS5 UDP support by sending DNS request to 8.8.8.8:53 and receive response.')
    parser.add_argument('--proxy', "-p",  metavar="PROXY", dest='proxy', required=True,
                       help='IP or domain name of proxy to be tested against UDP support.')
    parser.add_argument('--port', "-P",  metavar="PORT", dest='port', type=ip_port, default=1080,
                       help='Port of proxy to be tested against UDP support.')
    parser.add_argument('--user', "-u", metavar="username", dest="user", default=None,
                       help='Specify username to be used for proxy authentication.')
    parser.add_argument('--pwd', "-k", metavar="password", dest="pwd", default=None,
                       help='Specify password to be used for proxy authentication.')
    args = parser.parse_args()
    test_udp(None, args.proxy, args.port, args.user, args.pwd)


if __name__ == "__main__":
    main()
