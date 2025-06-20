import pandas as pd
import matplotlib.pyplot as plt
import cartopy.crs as ccrs
import cartopy.io.shapereader as shpreader
import numpy as np

# Load CSV file
df = pd.read_csv('app/tle_output.csv')

lat = df['Latitude(deg)'].values
lon = df['Longitude(deg)'].values

# --- Plot Map Setup ---
plt.figure(figsize=(12, 6))
ax = plt.axes(projection=ccrs.PlateCarree())
ax.set_global()

# Static layers (Land, Ocean, Borders)
land_shp = shpreader.Reader('C:/Users/Adm Local/.local/share/cartopy/shapefiles/natural_earth/physical/ne_110m_land/ne_110m_land.shp')
ocean_shp = shpreader.Reader('C:/Users/Adm Local/.local/share/cartopy/shapefiles/natural_earth/physical/ne_110m_ocean/ne_110m_ocean.shp')
borders_shp = shpreader.Reader('C:/Users/Adm Local/.local/share/cartopy/shapefiles/natural_earth/cultural/ne_110m_admin_0_boundary_lines_land/ne_110m_admin_0_boundary_lines_land.shp')

ax.add_geometries(land_shp.geometries(), ccrs.PlateCarree(), facecolor='lightgray', edgecolor='none')
ax.add_geometries(ocean_shp.geometries(), ccrs.PlateCarree(), facecolor='lightblue', edgecolor='none')
ax.add_geometries(borders_shp.geometries(), ccrs.PlateCarree(), facecolor='none', edgecolor='gray', linestyle=':')

ax.gridlines(draw_labels=True, linewidth=0.5, color='gray', alpha=0.5, linestyle='--')

# --- Main Logic: Plot Without Longitude Jump ---
threshold = 180  # degrees

for i in range(1, len(lon)):
    lon_diff = abs(lon[i] - lon[i-1])
    if lon_diff < threshold:
        # Draw segment only if no longitude jump
        ax.plot([lon[i-1], lon[i]], [lat[i-1], lat[i]],
                color='red', linewidth=1.2, transform=ccrs.Geodetic())

plt.title('Satellite Ground Track (Longitude Jump Corrected)')
plt.show()
