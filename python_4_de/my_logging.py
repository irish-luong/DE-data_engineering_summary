import logging
import os
from os import path

import pandas as pd

CWD = os.getcwd()

logging.basicConfig(
    filename=path.join(CWD, "logging.log"),
    level=logging.DEBUG,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

df = pd.read_csv(
    path.join(CWD, "data.csv"),
    encoding='unicode_escape',
    nrows=1000
)

print(df)

logging.warning("Warning")
logging.info("Info")
logging.debug("Debug")
logging.error("Error")
logging.critical("Critical")
logging.exception("Exception")