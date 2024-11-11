import matplotlib.pyplot as plt
import numpy as np
from scipy.stats import gaussian_kde
from scipy.signal import find_peaks
import pandas as pd
import os

def get_peaks():
    os.chdir('/storage/emulated/0/Documents/Limitly/')
    # print out the contents of the current directory
    # print(os.listdir())
    with open('data.txt') as f:
        data = f.readlines()
    df = pd.DataFrame([sub.split(",") for sub in data])
    # make header the first row
    df.columns = df.iloc[0]
    df['Duration'] = df[' Duration\n'].str.replace('\n', '')
    # drop Duration\n
    df = df.drop(columns=[' Duration\n'])

    df = df[1:]
    # print(df.head())
    df.columns = df.columns.str.strip()

    df['Hour'] = df['Start Time'].apply(lambda x: pd.to_datetime(x).hour +  pd.to_datetime(x).minute/60)
    # # Assuming df is a pandas DataFrame already loaded with 'Hour' and 'Package Name' columns
    df['Hour'] = pd.to_numeric(df['Hour'], errors='coerce')
    df['Package Name'] = df['Package Name'].astype('category')

    # Create the plot
    plt.figure(figsize=(15, 6))

    # Get unique package names
    package_names = df['Package Name'].cat.categories

    # Store peak information
    peak_info = []

    # Colors for different packages
    colors = plt.cm.tab10(np.linspace(0, 1, len(package_names)))

    for i, package_name in enumerate(package_names):
        # Filter data for the current package
        package_data = df[df['Package Name'] == package_name]['Hour'].dropna()

        # Compute KDE using scipy's gaussian_kde
        kde = gaussian_kde(package_data)
        x_data = np.linspace(0, 24, 1000)
        y_data = kde(x_data)

        # Plot KDE
        plt.plot(x_data, y_data, label=package_name, color=colors[i])

        # Find peaks using scipy's find_peaks
        peaks, _ = find_peaks(y_data)

        # Store peak information
        peak_info.append({
            'package_name': package_name,
            'peak_x': x_data[peaks],
            'peak_y': y_data[peaks],
            'color': colors[i]
        })

        # Plot peaks as scatter points
        plt.scatter(x_data[peaks], y_data[peaks], color=colors[i], s=100, zorder=3)

    # Customize the plot
    plt.title('Frequency of Entries For Each Package Name with Peaks')
    plt.xlabel('Time of Day')
    plt.ylabel('Density')
    plt.xlim(0, 24)  # Set x-axis limits

    # Set x-axis ticks
    plt.xticks(np.arange(0, 25, 1), rotation=45)

    # Move the legend outside the plot
    plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left', title='Package Name')

    # Adjust layout to make room for the annotations
    plt.savefig('Top 10 Packages.png', bbox_inches='tight')


    peaks = pd.DataFrame(peak_info)
    # if more than one peak turn into long data
    peaks = peaks.explode('peak_x')
    peaks['TI_start'] = peaks['peak_x'].apply(lambda x: x-0.5)
    peaks['TI_end'] = peaks['peak_x'].apply(lambda x: x+0.5)
    peaks = peaks[['package_name', 'TI_start', 'TI_end']]

    # if TI_start is less than 0, add 24
    peaks['TI_start'] = peaks['TI_start'].apply(lambda x: x+24 if x<0 else x)
    peaks['TI_end'] = peaks['TI_end'].apply(lambda x: x+24 if x<0 else x)

    peaks.to_csv('peaks.txt', index=False)

    return "hi"