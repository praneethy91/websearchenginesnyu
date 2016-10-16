# Web Search Engines
Web search engines project started in Fall 2016 at New York University with multiple rankers, evaluators, Indexer, Query parser and more to come...

## Getting Started
These instructions will get you up and running with respect to compiling the code, generating the index file and starting the search engine. Then you can issue queries to it via various input channels and get your output as HTTP or in a results file. You can also run search engine evalutors on it.

### Prerequisities
You would need Git, Java Development Kit(JDK) installed. If you are using Windows you would also need to install [cURL](https://curl.haxx.se/download.html) if you want to issue queries/generate results via termainal/command line(You can issue queries/generate results via browser as well).

### Configuring
- Open terminal and run command from the directory in which you want to clone this repo.
```
git clone https://github.com/praneethy91/websearchenginesnyu.git
```
- In the terminal move into the root directory of the repo. Run all subsequent commands from this directory.
- Run command to compile all source files
```
javac src/edu/nyu/cs/cs2580/*.java
```
- Generate the index file.
```
java -cp src edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf
```
- Serve the search engine.
```
java -cp src -Xmx512m edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25802 --options=conf/engine.conf
```

### Generating Results File/Running Queries/Evaluation
- Open a new Terminal/Command prompt and move into the root of the Git repository as before.
- Generate results for a particular ranker as below. Puts the results in a tsv file depending on the ranker in the [githubreporoot]/results directory.
```
curl "http://localhost:25802/search?queryfile=data%2Fqueries.tsv&ranker=<rankerType>&format=text&output=file"
```
- If you don't want to generate results file from a query file for a ranker, and just want to check the top 10 results for a ranker for a particular query, you can do as follows:
```
curl "http://localhost:25802/search?query=<yourQuery>&ranker=<rankerType>&format=text"
```

### Ranker Types
Put in one of these possible values for the `<rankerType>`

1. cosine
2. ql
3. phrase
4. numviews
5. linear

### Input Types
You can give direct query input with the following CGI argument
```
query=your%20query
```
Or you can give a .tsv file containing the query with query words seperated by tabs and each query seperated by a line
```
queryfile=data%2fqueries.tsv
```
### Output Types
Two output format types are supported. Default is http if not provided.

1. http - (returns an http response)
2. file - (writes to a results file in the [githubreporoot]/results directory)

For example, you input as the following cgi-argument:
```
output=http
```
### Output Format
Two output format types are supported. Default is text if not provided.

1. html
2. text

For example, you input as the following cgi-argument:
```
format=text
```
### Number of ranked documents
You can pass in a value which determined how many ranked documents are returned. Default is 10.

For example, you input as the following cgi-argument to display top 30 results.
```
num=30
```
For example, you input as the following cgi-argument to display all documents in corpus in ranking order.
```
num=all
```

### Authors
* **Praneeth Yenugutala** - [Profile](https://github.com/praneethy91)
* **Sanketh Purwar**
* **Mansi Virani**

### License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

### Acknowledgements
[Cong Yu](https://sites.google.com/site/congyu/home)
</br>
[Fernando Diaz](http://msr.nyc/fdiaz/)
