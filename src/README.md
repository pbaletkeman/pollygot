# Race conditions

Race conditions occur when many threads read the same data at the same time and update that value.
Once value will be missed/ignored.

> ThreadA reads "orderStatus"
> ThreadB reads "orderStatus"

Possibilities:

**Missed ThreadA state**

1. ThreadA updates the value of the order to "proccessed"
2. ThreadB updates the value of the order to "cancelled"

The "processed" state is ignored

**Missed ThreadB state**

1. ThreadB updates the value of the order to "cancelled"
2. ThreadA updates the value of the order to "proccessed"

The "cancelled" state is ignored
