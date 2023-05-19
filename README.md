# cumulocity-iec104
Cumulocity agent used for IEC 60870-5-104 protocol.
This agent uses OpenMUC j60870 library, which is under GPLv3.
It needs to run in the same network as the "controller station", since it requires access to it, and needs an internet access if connected to SaaS Cumulocity instance.
To run properly the agent will need a configuration file to map ASDUs to Cumulocity model. A basic configuration file is provided in src/resources/asdu-example.json.
The agent stores the decoded ASDUs in a local SQLite instance, then poll the DB to send data to Cumulocity through MQTT at configurable interval.
Default agent configuration can be found here: src/resources/META-INF/spring/iec104-agent-gateway.properties

-------
These tools are provided as-is and without warranty or support. They do not constitute part of the Software AG product suite. Users are free to use, fork and modify them, subject to the license agreement. While Software AG welcomes contributions, we cannot guarantee to include every contribution in the master project.
