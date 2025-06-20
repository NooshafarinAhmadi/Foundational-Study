import pandas as pd
import lightgbm as lgb
from sklearn.model_selection import train_test_split
import shap
import matplotlib.pyplot as plt

# Load your dataset
df = pd.read_csv("orbit_output.csv")

# Clean column names
df.columns = [col.split("(")[0].strip() for col in df.columns]

# Drop non-numeric columns
df = df.drop(columns=["Date"])

# Remove rows with NaN
df = df.dropna()

# Define features and target
X = df.drop(columns=["Altitude"])
y = df["Altitude"]

# Train/test split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Train LightGBM
model = lgb.LGBMRegressor()
model.fit(X_train, y_train)

# SHAP explanation
explainer = shap.Explainer(model, X_train)
shap_values = explainer(X_test)

# Beeswarm plot
shap.plots.beeswarm(shap_values)

# Bar plot
shap.plots.bar(shap_values)
