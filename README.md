# Bedrock Bridge Service

The Bedrock Bridge Service is a specialized tool designed to fetch and retrieve data from Layer 1 (L1) and Layer 2 (L2) on the Ethereum network, with a specific focus on the Optimism Bedrock stack bridge. This service is tailored to interact with L1 and L2 RPC endpoints to gather relevant bridge data, streamlining the process of obtaining transactional and bridge-specific information in the context of Ethereum's Optimism Bedrock upgrade. Leveraging Docker for straightforward deployment, the service also includes an API that facilitates the retrieval and querying of this bridge data, making it an essential tool for those working with Ethereum's L1 and L2 bridging mechanisms.

## Features

- Specializes in fetching data from L1/L2 RPC endpoints in the Ethereum network.
- Docker integration for simplified and consistent deployment.
- Provides a comprehensive API for querying and accessing bridging transaction data.

## Prerequisites

Before you begin, ensure you have installed:
- Docker and Docker Compose
- Git

## Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/socialscan-io/bedrock-bridge-service.git
   ```
2. **Set Up Environment Variables**
    ```bash
   # Modify the .env file in the project root with your specific Ethereum RPC URLs. 
   # L1_RPC_URL=https://mainnet.infura.io/v3/your_l1_rpc_url
   # L2_RPC_URL=https://mainnet.infura.io/v3/your_l2_rpc_url
   # BATCH_RPC_URL=https://mainnet.infura.io/v3/your_batch_rpc_url
   source .env 
   ```
3. **Build the Docker Image**
    ```bash
    docker build --no-cache -t lifo/ethereum-sync:latest .
    ```
4. **Start the Docker Container**
    ```bash
    docker-compose up -d
    ```
5. **Query the API**
    ```bash
    curl --location --request GET 'http://127.0.0.1:8080/v1/explorer/l2_to_l1_transactions?size=50&page=1' --data-raw ''
   ```
## API Documentation
1. **v1/explorer/l2_to_l1_transactions**
- **Method:** GET
- **Description:** Get L2 to L1 transactions
- **Parameters:**
    - **size** (optional): The number of transactions to return. Default: 50
    - **page** (optional): The page number. Default: 1
    - **address** (optional): The address to filter by to_address

2. **v1/explorer/l1_to_l2_transactions**
- **Method:** GET
- **Description:** Get L1 to L2 transactions
- **Parameters:**
    - **size** (optional): The number of transactions to return. Default: 50
    - **page** (optional): The page number. Default: 1
    - **address** (optional): The address to filter by to_address

## Additional Notes
### About L2 to L1 Transactions(Withdrawal) Status
The L2 to L1 transactions status is indicated by the `status` field in the response. The status can be one of the following:

1. **waiting**: The L2 transaction is pending and has not been confirmed on L1.
2. **ready_to_prove**: The transaction is ready to be proven on L1.
3. **in_challenge_period**: The transaction is in the challenge period on L1. waiting for the challenge period to end. The Manta-Pacific Bridge has a **3**-day challenge period.
4. **ready_to_finalize**: The transaction is out of the challenge period and ready to be finalized on L1.
5. **relayed**: The transaction has been relayed to L1.

## Documentation

For more detailed information about the deposit and withdrawal flows in the Optimism network, refer to the following documents:
- [Optimism Deposit Flow Documentation](https://docs.optimism.io/stack/protocol/deposit-flow)
- [Optimism Withdrawal Flow Documentation](https://docs.optimism.io/stack/protocol/withdrawal-flow)
