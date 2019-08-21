#!/bin/sh

rm -f key.pem cert.pem

openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem <<EOF
UK
.
London
R3
.
localhost
dev@r3.com
EOF

