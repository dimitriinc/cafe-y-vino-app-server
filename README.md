# Cafe y Vino App Server

An XMPP client for the FCM backend.
All credit goes to [carlosCharz](https://github.com/carlosCharz){:target="\_blank"} and his [project](https://github.com/carlosCharz/fcmxmppserverv2){:target="\_blank"}.
Slightly simplified and adjusted for the needs of the Cafe y Vino system.

## Summary

The purpose of the client is to link the [customer side](https://github.com/dimitriinc/cafe-y-vino-app-client){:target="\_blank"} of the Cafe y Vino system to the [administrator side](https://github.com/dimitriinc/cafe-y-vino-app-admin){:target="\_blank"}, so that they can exchange messages.
Connects to FCM backend through a TCP connection and listens for upstream messages. On receiving one, sends back an ACK to the FCM and checks the type of the message, stored in the data payload. There are two types:

- from customer to admin message - on reception: a downstream stanza is created with the data payload copied from the upstream message, and sent to the array of administrators' registration tokens, one for each administrator;
- from admin to customer message - on reception: the recipient customer's registration token is extracted from the data payload, and a downstream stanza, directed to this token, is created and sent with the data payload copied from the upstream message.

Handles Connection Draining messages and reconnections.

## Techonologies

- Java 1.8
- Smack API 4.3.4
- Jackson API 2.11.0

## License

This project is licensed under the terms of the MIT license
