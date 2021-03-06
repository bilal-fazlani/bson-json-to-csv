# bson-json-to-csv

Creates a flat csv files out of a nested bson/json file

## [Download](https://github.com/bilal-fazlani/bson-json-to-csv/releases/latest/download/bson-json-to-csv)


[![GitHub Release Date](https://img.shields.io/github/release-date/bilal-fazlani/bson-json-to-csv?style=for-the-badge)](https://github.com/bilal-fazlani/bson-json-to-csv/releases/latest)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/bilal-fazlani/bson-json-to-csv?color=blue&label=version&sort=semver&style=for-the-badge)](https://github.com/bilal-fazlani/bson-json-to-csv/releases/latest/download/bson-json-to-csv)



## Usage

![help](/images/help.png)

![help](/images/usage.png)

Works with streaming of objects. For example,

```json
{
  "name": "john",
  "location": {
    "city": "mumbai",
    "country": "india"
  }
}
{
  "name": "jane",
  "location": "delhi",
  "tags": ["scala", "java", "big data"]
}
```

each top level object results into one csv row. The above file will generate a csv with a header and two csv records

```csv
.name,.location.city,.location.country,.tags[1],.location,.tags[0],.tags[2]
john,mumbai,india,,,,
jane,,,java,delhi,scala,big data
```

The headers json-paths of corresponding json fields

Notice that as show in above example, the order of columns may not be same as order of json fields



