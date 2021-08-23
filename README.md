# points

This repo is my solution to the Fetch Rewards coding exercise.

## Goal

Write a simple web service for bookkeeping points per payer. Points are added via transactions. Each transaction has the payer, the number of points, and the timestamp. When using some points, start from the oldest transaction.

## Design Decisions

I've decided to use a priority queue for an easier implementation. In the real world, we'll probably want to use some persistent storage.

A payer is allowed to have a total number of negative points temporarily since a later transaction might make up for it. Similarly, a transaction with zero points is also allowed.

If there aren't enough points to use, the response will be 400 Bad Request with an error message.

## RESTful API

- `POST /points/v1/transactions`: Add a transaction.
- `POST /points/v1/balance`: Use some points.
- `GET /points/v1/balance`: Get a balance summary for points per payer.

## Known Issues

- Race condition might happen if multiple requests are reading and writing the core data structure at the same time.

## Future Ideas

- Improve known issues.
- Consider adding the OpenAPI Specification and the Swagger UI.

## Prepare Tools

This repo needs the following tools:

- Java 11: https://openjdk.java.net/install/index.html or https://adoptopenjdk.net/
- sbt 1.5.5: https://www.scala-sbt.org/download.html

Note that you may have multiple versions of Java installed. **Make sure** that you are using version 11 for this repo.

Below are some tips for each platform.

### Linux

Use your distribution's package manager to install those packages. For example, on Arch Linux:

```
sudo pacman -S jdk11-openjdk sbt
```

### macOS

Use [HomeBrew](https://brew.sh/) to install those packages. For example:

```
brew tap AdoptOpenJDK/openjdk
brew install --cask adoptopenjdk11
brew install sbt
```

### Windows

I haven't tried any Windows package manager at this time. Maybe it's easier to go to the tools' official websites, and just follow the instructions there.

## Build and Run

Clone the repo using git, or download a ZIP file of the repo and extract it. Open the repo's root folder in a terminal window. In other words, you should see the file `README.md` if you list the current folder. The following command will rebuild the repo, run unit tests with coverage, generate a coverage report, and finally start the web server.

```
sbt clean coverage test coverageReport run
```

The web server will be ready when you see something like `(Server started, use Enter to stop and go back to the console...)`.

## Test

Use cURL or your favorite RESTful API client to test the endpoints. **Note** that the very first call may be slow due to additional initialization. Here is a simple test case:

1. Add a transaction:
    ```
    curl --request POST \
      --url http://localhost:9000/points/v1/transactions \
      --header 'Content-Type: application/json' \
      --data '{ "payer": "payer1", "points": 1000, "timestamp": "2021-08-23T00:00:00Z" }'
    ```
1. Add another transaction:
    ```
    curl --request POST \
      --url http://localhost:9000/points/v1/transactions \
      --header 'Content-Type: application/json' \
      --data '{ "payer": "payer2", "points": 2000, "timestamp": "2021-08-23T01:00:00Z" }'
    ```
1. Use some points:
    ```
    curl --request POST \
      --url http://localhost:9000/points/v1/balance \
      --header 'Content-Type: application/json' \
      --data '{ "points": 1500 }'
    ```
1. At this time you should see a response like:
    ```
    [
      {
        "payer": "payer1",
        "points": -1000
      },
      {
        "payer": "payer2",
        "points": -500
      }
    ]
    ```
1. Get the points balance:
    ```
    curl --request GET \
      --url http://localhost:9000/points/v1/balance
    ```
1. Now you should see a response like:
    ```
    {
      "payer1": 0,
      "payer2": 1500
    }
    ```
1. Press Enter if you want to terminate the web server in the terminal window.
