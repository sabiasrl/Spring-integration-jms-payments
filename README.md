## Spring-boot Integration JMS Payments

Demo of an Integration and BPM flow using Spring Integration DLS with Fluent Builder API.  
The integration flow implements a simple banking payment scenario simulating a **messaging hub** which send transactions from a mapped **network** to a **gateway**.  
JMS adaptors are connected to the integration flow working with ArtemisQM JMS destinations.

![Integration Platform](IP.png)

- JMS destinations of the NETWORK are mapped into integration message channels
- Messages are routed by the message format type
- Specific validation of the transaction's payload based on the message format type
- All transactions are sent to the specific messaging gateway
- Integration message channels are mapped into JMS destinations of the GATEWAY system

### CLI

##### verify
```shell
mvn clean verify
```

##### run demo
```shell
mvn clean install -DskipTests spring-boot:run 
```
