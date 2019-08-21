FROM apache/nifi
USER root
#RUN apt-get update && apt-get install -y sudo
#RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
RUN  usermod -a -G uucp nifi && usermod -a -G dialout nifi && usermod -a -G tty nifi
USER nifi 
COPY target/nifi-serial-nar-0.1-SNAPSHOT.nar /opt/nifi/nifi-current/lib/
COPY jSerialComm-2.5.1.jar /opt/nifi/nifi-current/lib/
