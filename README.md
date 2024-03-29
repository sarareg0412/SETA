# SETA
SETA (SElf-driving TAxi service) is a peer-to-peer system of self-driving taxis developed mostly in Java, for the citizens of a smart city.
The citizens of the smart city use SETA to request a self-driving taxi that takes them from their current position to a random destination point in the map.

The following applications were developed:

• MQTT Broker: the MQTT broker that communicates the ride requests to the taxis
• SETA: a process that simulates the taxi service requests generated by the citizens that must be communicated to the fleet of self-driving taxis through the MQTT component
• Taxi: a specific self-driving taxi of the system
• Administrator Server: REST server that receives statistics from the smart city taxis and dynamically adds/removes taxis from the network
• Administrator Client: a client that queries the Administrator Server to obtain information about the statistics of the taxis and their rides
