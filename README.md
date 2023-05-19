# cumulocity-iec104
Cumulocity agent used for IEC 60870-5-104 protocol.

This agent uses OpenMUC j60870 library, which is under GPLv3.

It needs to run in the same network as the "controller station", since it requires access to it, and needs an internet access if connected to a SaaS Cumulocity instance.

To run properly, the agent will need a configuration file to map ASDUs to Cumulocity model. A basic configuration file is provided in src/resources/asdu-example.json.

The agent can use either HTTP or MQTT to send data to Cumulocity by setting the c8y.api.mode property to http or mqtt.

The agent can store the decoded ASDUs in a local SQLite instance (buffer.enabled=true). In that case another piece of software will be responsible for polling the DB to send data to Cumulocity, so if the agent runs as standalone set this property to false.

Default agent configuration can be found here: src/resources/META-INF/spring/iec104-agent-gateway.properties.

Custom configurations can be stored in ${user.home}/.iec104, or, if the agent is configured to run as a service using systemd, in /etc/iec104.

Agent configuration file name is iec104-agent-gateway.properties

ASDU mapping configuration file name is asdu.json

To connect the agent to a Cumulocity instance you just need to register a new device in the target Cumulocity instance using the agent id as configured in iec104-agent-gateway.properties (property gateway.identifier).

You can log all incoming ASDUs in Cumulocity as events by setting the property log.enabled to true.

-------
These tools are provided as-is and without warranty or support. They do not constitute part of the Software AG product suite. Users are free to use, fork and modify them, subject to the license agreement. While Software AG welcomes contributions, we cannot guarantee to include every contribution in the master project.
