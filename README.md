# mqtt spring boot starter

## Features

- [x] MQTT v5
- [ ] ~~MQTT v3.1~~

- [x] Subscribe
- [ ] Publish

- [x] Wildcards
- [x] Shared subscriptions

## Not Supported

- QoS(only supported Qos=0)

## Notice

- ~~If using both wildcards topics (`mqtt/#`, `mqtt/+`) and exact-match topics (`mqtt/1`) can cause problems with
  duplicate messages being received.~~

## How to use

see [mqtt demo](https://github.com/ming-lz/mqtt-demo)
