## Bedrock Bridge Service

## How to run
```bash
$ git clone https://github.com/socialscan-io/bedrock-bridge-service.git
## change .env file 
# L1_RPC_URL=https://mainnet.infura.io/v3/test
# BATCH_RPC_URL=https://mainnet.infura.io/v3/test
$ source .env
$ docker build -t  lifo/ethereum-sync:latest .
$ docker-compose up -d
$ curl --location --request GET 'http://127.0.0.1:8080/v1/explorer/l2_to_l1_transactions?size=50&page=1' \
--data-raw ''
```