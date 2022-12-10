FROM node:latest
WORKDIR /usr/app
COPY . /usr/app
RUN rm -rf node_modules &&\
	npm install &&\
	npm install -g artillery &&\
	npm i @faker-js/faker &&\
	npm install -g node-fetch -save &&\
	npm install -g https://github.com/preguica/artillery-plugin-metrics-by-endpoint.git
	
CMD artillery run --output report-test-iamges.json test-images.yml &&\
	artillery run --output report-create-users.json create-users.yml &&\
	artillery run --output report-create-auctions.json create-auctions.yml &&\
	artillery run --output report-workload1.json workload1.yml
	