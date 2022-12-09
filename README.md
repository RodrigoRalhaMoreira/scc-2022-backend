# scc-2022-backend

To run the code you compile it:
mvn clean compile assembly:single

Then you deploy it:
az login
az account set --subscription "887bf2f2-2f25-4a70-99b4-528dc74419f3"
mvn clean compile package azure-webapp:deploy

Then you can make the requests on postman

To run azure functions:
-> Change pom from <packaging>war<packaging> to <packaging>jar<packaging>
-> run: mvn clean compile package azure-functions:deploy

Developed by:

Rodrigo Moreira 57943 - rr.moreira@campus.fct.unl.pt

Dinis Silvestre 58763 - dj.silvestre@campus.fct.unl.pt 

Tiago Duarte 58125 - tj.duarte@campus.fct.unl.pt
