package uk.co.truenotfalse.model;

import java.time.OffsetDateTime;
import java.util.Objects;


/**
 *  Represents a period of outage for a device with given ID.
 */
public class DeviceOutage
{
    /**
     *  Creates an instance of this class.
     *
     *  @see #getId()
     *  @see #getBegin()
     *  @see #getEnd()
     */
    public DeviceOutage(final Object id, final OffsetDateTime begin, final OffsetDateTime end)
    {
        Objects.requireNonNull(id, "An object ID is required.");
        Objects.requireNonNull(begin, "The beginning of an outage is required.");
        // I assume an outage can be ongoing and that this is indicated by a null value?

        this.id = id;
        this.begin = begin;
        this.end = end;
    }


    /**
     *  The ID of the device that experienced the outage.
     */
    public Object getId()
    {
        return id;
    }

    /**
     *  The beginning of the outage period.
     */
    public OffsetDateTime getBegin()
    {
        return begin;
    }

    /**
     *  The end of the outage period.  May be {@code null}.
     */
    public OffsetDateTime getEnd()
    {
        return end;
    }


    /**
     *  The name of the device.  May be {@code null}.
     */
    public String getDeviceName()
    {
        return deviceName;
    }


    /**
     *  @see #getDeviceName()
     */
    // Generally, I prefer immutable value and data objects but has this is a simple app with a transient life-time,
    // I have gone with a partially mutable object.
    public void setDeviceName(final String name)
    {
        // For later initialization so do not allow to be set to null.
        Objects.requireNonNull(name, "A device name is required.");

        if(name.isBlank())
        {
            // Assume a device name of only whitespace is not meaningful.
            throw new IllegalArgumentException("A meaningful device name is required.");
        }

        this.deviceName = name;
    }


    @Override
    public boolean equals(final Object rhs)
    {
        if(this == rhs)
        {
            return true;
        }
        if(rhs == null || getClass() != rhs.getClass())
        {
            return false;
        }

        final DeviceOutage that = (DeviceOutage)rhs;
        return id.equals(that.id) && begin.equals(that.begin) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, begin, end);
    }


    @Override
    public String toString()
    {
        return "{id=" + id + ", begin=" + begin + ", end=" + end + ", deviceName='" + deviceName + '\'' + '}';
    }


    private final Object id;
    private final OffsetDateTime begin;
    private final OffsetDateTime end;
    private String deviceName;
}
