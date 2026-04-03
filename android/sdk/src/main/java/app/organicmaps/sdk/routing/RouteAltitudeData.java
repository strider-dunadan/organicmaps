package app.organicmaps.sdk.routing;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

public class RouteAltitudeData
{
  @NonNull
  private final double[] mDistances; // meters from start
  @NonNull
  private final int[] mAltitudes; // meters elevation
  @NonNull
  private final double[] mLats;
  @NonNull
  private final double[] mLons;

  private final int mTotalAscent;
  private final int mTotalDescent;
  private final int mMinAltitude;
  private final int mMaxAltitude;

  // Called from JNI.
  @Keep
  public RouteAltitudeData(@NonNull double[] distances, @NonNull int[] altitudes, @NonNull double[] lats,
                           @NonNull double[] lons, int totalAscent, int totalDescent, int minAltitude, int maxAltitude)
  {
    if (distances.length != altitudes.length || distances.length != lats.length || distances.length != lons.length)
      throw new IllegalArgumentException("All arrays must have the same length");
    mDistances = distances;
    mAltitudes = altitudes;
    mLats = lats;
    mLons = lons;
    mTotalAscent = totalAscent;
    mTotalDescent = totalDescent;
    mMinAltitude = minAltitude;
    mMaxAltitude = maxAltitude;
  }

  public int getSize()
  {
    return mDistances.length;
  }

  public double getDistance(int index)
  {
    return mDistances[index];
  }

  public int getAltitude(int index)
  {
    return mAltitudes[index];
  }

  public double getLat(int index)
  {
    return mLats[index];
  }

  public double getLon(int index)
  {
    return mLons[index];
  }

  public int getTotalAscent()
  {
    return mTotalAscent;
  }

  public int getTotalDescent()
  {
    return mTotalDescent;
  }

  public int getMinAltitude()
  {
    return mMinAltitude;
  }

  public int getMaxAltitude()
  {
    return mMaxAltitude;
  }
}
