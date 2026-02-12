import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import argparse
import os


def generate_graphs(jtl_file, output_dir):
    """
    Parses JMeter JTL/CSV results and generates performance graphs.
    """
    if not os.path.exists(jtl_file):
        print(f"Error: Results file '{jtl_file}' not found.")
        return

    print(f"Loading data from {jtl_file}...")
    try:
        # Load JMeter CSV
        df = pd.read_csv(jtl_file)

        # Convert timestamp to datetime (JMeter uses milliseconds)
        # Handle potential errors if format is unexpected
        if "timeStamp" in df.columns:
            df["datetime"] = pd.to_datetime(df["timeStamp"], unit="ms")
        else:
            print("Error: 'timeStamp' column missing from JTL file.")
            return

        # Create output directory
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        # Set style
        sns.set_style("whitegrid")
        plt.rcParams.update({"font.size": 12})

        # --- 1. RPS Over Time ---
        print("Generating RPS Graph...")
        df["second"] = df["datetime"].dt.floor("S")
        rps = df.groupby("second").size()

        plt.figure(figsize=(14, 7))
        plt.plot(rps.index, rps.values, label="Total RPS", color="#2ecc71", linewidth=2)

        # Overlay Errors
        errors = df[df["responseCode"] != 200].groupby("second").size()
        if not errors.empty:
            plt.plot(
                errors.index,
                errors.values,
                label="Errors (Non-200)",
                color="#e74c3c",
                linewidth=2,
            )

        plt.title("Requests Per Second (RPS) Over Time", fontsize=16)
        plt.xlabel("Time")
        plt.ylabel("RPS")
        plt.legend()
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "rps_over_time.png"), dpi=300)
        plt.close()

        # --- 2. Latency Distribution (Histogram) ---
        print("Generating Latency Histogram...")
        plt.figure(figsize=(12, 6))
        # Filter outliers for better visualization (< 99th percentile)
        p99 = df["elapsed"].quantile(0.99)
        filtered_df = df[df["elapsed"] <= p99]

        sns.histplot(
            filtered_df["elapsed"], bins=50, kde=True, color="#3498db", alpha=0.6
        )

        # Add percentile lines
        p50 = df["elapsed"].median()
        p95 = df["elapsed"].quantile(0.95)

        plt.axvline(p50, color="green", linestyle="--", label=f"P50: {p50:.1f}ms")
        plt.axvline(p95, color="orange", linestyle="--", label=f"P95: {p95:.1f}ms")
        plt.axvline(p99, color="red", linestyle="--", label=f"P99: {p99:.1f}ms")

        plt.title("Response Time Distribution (Latency)", fontsize=16)
        plt.xlabel("Response Time (ms)")
        plt.ylabel("Frequency")
        plt.legend()
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "latency_histogram.png"), dpi=300)
        plt.close()

        # --- 3. Latency Over Time (Avg & P95) ---
        print("Generating Latency Trends...")
        latency_agg = df.groupby("second")["elapsed"].agg(
            Avg="mean", P95=lambda x: np.percentile(x, 95)
        )

        plt.figure(figsize=(14, 7))
        plt.plot(
            latency_agg.index,
            latency_agg["Avg"],
            label="Average Latency",
            color="#9b59b6",
            alpha=0.8,
        )
        plt.plot(
            latency_agg.index,
            latency_agg["P95"],
            label="P95 Latency",
            color="#e67e22",
            linestyle="--",
        )

        plt.title("Latency Trends Over Time", fontsize=16)
        plt.xlabel("Time")
        plt.ylabel("Latency (ms)")
        plt.legend()
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "latency_trend.png"), dpi=300)
        plt.close()

        print(f"Graphs successfully saved to: {os.path.abspath(output_dir)}")

    except Exception as e:
        print(f"An error occurred: {e}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="VelocityGate Load Test Visualizer")
    parser.add_argument(
        "jtl_file",
        nargs="?",
        default="results/results.jtl",
        help="Path to JMeter results file (.jtl or .csv)",
    )
    parser.add_argument("--output", default="graphs", help="Directory to save graphs")
    args = parser.parse_args()

    generate_graphs(args.jtl_file, args.output)
