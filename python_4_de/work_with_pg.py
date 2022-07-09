# Prepare for this part
# pip install psycopg2-binary
# have installed Docker engine
# execute docker-compose.yml to stat experiment stack

# Pure Python libraries
import json
import jsonschema
from typing import Tuple, Optional

# 3PL libraries
import psycopg2
import pandas as pd
import pandas.io.sql as sqlio

# Constant
PG_DBNAME = "psycopg"
PG_USER = "root"
PG_PASSWORD = "root"
PG_HOST = "localhost"


class PGConnection:

    def __init__(self, connection_info: dict):
        # Connection configuration
        self.connection_info = connection_info
        self.conn = None

    def __enter__(self) -> psycopg2.extensions.connection:
        print(f"Open connection to database {self.connection_info['host']}")
        self.conn = psycopg2.connect(**self.connection_info)
        return self.conn

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.conn:
            self.conn.close()
        print("Close connection")


def validate_json_schema(schema: dict, data: dict) -> Tuple[int, Optional[str]]:
    """
        Function validate a JSON data by a provided JSON schema
        Ref: https://python-jsonschema.readthedocs.io/en/stable/
        Args:
            schema: conventional JSON schema format
            data: data want to validate with the schema

        Return:
            Tuple of error code and error message
            If error code then return error code is not equal to 0
    """

    try:
        jsonschema.validate(instance=data, schema=schema)
    except jsonschema.exceptions.ValidationError as err:
        return 1, err.__str__()

    return 0, None


def validate_json(data: str) -> bool:
    """
        Function parse a dictionary string to JSON format
        Args:
            data: a dictionary of data
    """
    try:
        json.loads(data)
    except ValueError:
        return False
    return True


def main(*args, **kwargs) -> None:
    # Connect to database
    sql_create_table = """
        CREATE TABLE IF NOT EXISTS transaction(
            id SERIAL PRIMARY KEY,
            InvoiceNo VARCHAR(255),
            StockCode VARCHAR(255),
            Description TEXT,
            Quantity REAL,
            InvoiceDate VARCHAR(255),
            UnitPrice REAL,
            CustomerID INTEGER,
            Country VARCHAR(255)
        )
    
    """

    sql_bulk_insert_data = """
        COPY transaction(InvoiceNo,StockCode,Description,Quantity,InvoiceDate,UnitPrice,CustomerID,Country)
        FROM '/mnt/data/data.csv' CSV HEADER;
    """

    sql_query_invoice = """
        SELECT 
            *
        FROM
            transaction
        WHERE
            InvoiceNo = '<invoice_no>'
    """

    with PGConnection(
            {
                "user": PG_USER,
                "host": PG_HOST,
                "dbname": PG_DBNAME,
                "password": PG_PASSWORD
            }
    ) as conn:
        cur = conn.cursor()

        # Create table if not exists
        cur.execute(sql_create_table)
        conn.commit()

        # Load data
        cur.execute(sql_bulk_insert_data)
        conn.commit()

        # Load data to pandas object
        df = sqlio.read_sql_query(
            sql_query_invoice.replace("<invoice_no>", "536370"),
            conn
        )
        print(df)


if __name__ == "__main__":
    main()
