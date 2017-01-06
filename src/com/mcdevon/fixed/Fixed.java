package com.mcdevon.fixed;

public final class Fixed {
	private final int _data;
	
	// Static values for 32-bit fixed-point value
	private static final int MAX_VALUE = Integer.MAX_VALUE;
	private static final int MIN_VALUE = Integer.MIN_VALUE;
	
	public static final int BITS = 32;
	private static final int FRACTIONAL_PLACES = 10;
	private static final int FRACTION_MASK = new Integer(-1) >>> (BITS - FRACTIONAL_PLACES);
	private static final int FULL_MASK = new Integer(-1);
	
	private static final int ONE = 1 << FRACTIONAL_PLACES;
	private static final int TWO = 1 << (FRACTIONAL_PLACES + 1);
	private static final int HALF = 1 << (FRACTIONAL_PLACES - 1);
	
	public static final Fixed maxValue = new Fixed(MAX_VALUE);
	public static final Fixed minValue = new Fixed(MIN_VALUE);
	public static final Fixed zero = new Fixed(0);
	public static final Fixed one = new Fixed(ONE);
	public static final Fixed two = new Fixed(TWO);
	public static final Fixed half = new Fixed(HALF);
	
	private Fixed(int data) {
		_data = data;
	}
	
	public static int sign(Fixed value) {
		return value._data < 0 ? -1 :
			value._data > 0 ? 1 : 0;
	}
	
	public static Fixed abs(Fixed value) {
		int mask = value._data >> (BITS - 1);
		return new Fixed((value._data + mask) ^ mask);
	}
	
	public static Fixed safeAbs(Fixed value) {
        if (value._data == MIN_VALUE) {
            return maxValue;
        }
		
		int mask = value._data >> (BITS - 1);
		return new Fixed((value._data + mask) ^ mask);
	}
	
	/*
	 * Operators
	 */
	
	public Fixed add(Fixed value) {
		return new Fixed(_data + value._data);
	}
	
	public Fixed sub(Fixed value) {
		return new Fixed(_data - value._data);
	}
	
	public Fixed mul(Fixed value) {
        int xl = _data;
        int yl = value._data;

        int xlo = xl & FRACTION_MASK;
        int xhi = xl >> FRACTIONAL_PLACES;
        int ylo = yl & FRACTION_MASK;
        int yhi = yl >> FRACTIONAL_PLACES;

        int lolo = xlo * ylo;
        int lohi = xlo * yhi;
        int hilo = xhi * ylo;
        int hihi = xhi * yhi;

        int loResult = lolo >>> FRACTIONAL_PLACES;
        int midResult1 = lohi;
        int midResult2 = hilo;
        int hiResult = hihi << FRACTIONAL_PLACES;

        int sum = loResult + midResult1 + midResult2 + hiResult;
        return new Fixed(sum);
    }
	
	private static final int oneBitHighMask = MIN_VALUE;
    private static final int fourBitHighMask = oneBitHighMask >> 3;
	
	private static int leadingZeroes(int x) {
        int result = 0;
        while ((x & fourBitHighMask) == 0) { result += 4; x <<= 4; }
        while ((x & oneBitHighMask) == 0) { result += 1; x <<= 1; }
        return result;
    }

    public Fixed div(Fixed value) {
        int xl = _data;
        int yl = value._data;

        if (yl == 0) {
            throw new ArithmeticException("Divide by zero");
        }

        int remainder = xl >= 0 ? xl : (-xl) & (~MIN_VALUE);
        int divider = yl >= 0 ? yl : (-yl) & (~MIN_VALUE);
        int quotient = 0;
        int bitPos = FRACTIONAL_PLACES + 1;

        // If the divider is divisible by 2^n, use bit shifts for faster calculation
        while ((divider & 0xF) == 0 && bitPos >= 4) {
            divider >>>= 4;
            bitPos -= 4;
        }

        while (remainder != 0 && bitPos >= 0) {
            int shift = leadingZeroes(remainder);
            if (shift > bitPos) {
                shift = bitPos;
            }
            remainder <<= shift;
            bitPos -= shift;

            int div = remainder / divider;
            remainder = remainder % divider;
            quotient += div << bitPos;

            // Detect overflow
            if ((div & ~(FULL_MASK >>> bitPos)) != 0) {
                return ((xl ^ yl) & MIN_VALUE) == 0 ? maxValue : minValue;
            }

            remainder <<= 1;
            bitPos--;
        }

        // rounding
        quotient++;
        int result = quotient >>> 1;
        if (((xl ^ yl) & MIN_VALUE) != 0) {
            result = -result;
        }

        return new Fixed(result);
    }
    
	public Fixed mod(Fixed value) {
		return new Fixed(_data % value._data);
	}
	
	public Fixed negate() {
		return new Fixed(-_data);
	}
	
	public boolean lessThan(Fixed value) {
		return _data < value._data;
	}
	
	public boolean moreThan(Fixed value) {
		return _data > value._data;
	}
	
	public boolean lessThanOrEquals(Fixed value) {
		return _data <= value._data;
	}
	
	public boolean moreThanOrEquals(Fixed value) {
		return _data >= value._data;
	}
	
	public boolean equals(Fixed value) {
		return _data == value._data;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
	        return false;
	    }
	    if (!(obj instanceof Fixed)) {
	        return false;
	    }
	    final Fixed other = (Fixed) obj;
	    return _data == other._data;
	}
	
	@Override
	public int hashCode() {
	    return _data;
	}
	
	/*
	 * Safe operators
	 */

	public Fixed safeAdd(Fixed value) {
        int xl = _data;
        int yl = value._data;
        int sum = xl + yl;
         
        // Check for overflows
        // TODO: Throw exception for overflow?
        if (((~(xl ^ yl) & (xl ^ sum)) & MIN_VALUE) != 0) {
            sum = xl > 0 ? MAX_VALUE : MIN_VALUE;
        }
        return new Fixed(sum);
	}
	
	public Fixed safeSub(Fixed value) {
        int xl = _data;
        int yl = value._data;
        int sub = xl - yl;
        
        // Check for overflows
        // TODO: Throw exception for overflow?
        if ((((xl ^ yl) & (xl ^ sub)) & MIN_VALUE) != 0) {
        	sub = xl < 0 ? MIN_VALUE : MAX_VALUE;
        }
        return new Fixed(sub);
	}
	
	/*
	 * Compatibility
	 */
	
	public static Fixed fromInt(int value) {
		return new Fixed(value * ONE);
	}
	
	public static Fixed fromLong(long value) {
		return new Fixed((int)value * ONE);
	}
	
	public static Fixed fromString(String stringValue) {
		String[] parts = null;
		String[] delimiters = {"\\.",","};

		for (int i = 0; i < delimiters.length && (parts == null || parts.length == 1); i++) {
			parts = stringValue.split(delimiters[i]);
		}

		if (parts.length > 2 || parts.length == 0) {
			throw new NumberFormatException("Invalid input string");
		}

		int left = Integer.parseInt(parts[0]);

		if (parts.length == 2) {

			int right = Integer.parseInt(parts[1]);

			Fixed divider = _tenPowerTable[parts[1].length()]; // 10 ^parts.Length
				
			Fixed fLeft = Fixed.fromInt(left);
			Fixed sign = fLeft.lessThan(Fixed.zero) ? Fixed.one.negate() : Fixed.one;

			return fLeft.add(Fixed.fromInt(right).div(divider).mul(sign));
		}

		return Fixed.fromInt(left);
	}

	private static Fixed[] _tenPowerTable = new Fixed[] {
			Fixed.fromLong(1L),  // 10^0
			Fixed.fromLong(10L),  // 10^1
			Fixed.fromLong(100L),  // 10^2
			Fixed.fromLong(1000L),  // 10^3
			Fixed.fromLong(10000L),  // 10^4
			Fixed.fromLong(100000L),  // 10^5
			Fixed.fromLong(1000000L),  // 10^6
			Fixed.fromLong(10000000L),  // 10^7
			Fixed.fromLong(100000000L),  // 10^8
			Fixed.fromLong(1000000000L),  // 10^9
			Fixed.fromLong(10000000000L),  // 10^10
			Fixed.fromLong(100000000000L),  // 10^11
			Fixed.fromLong(1000000000000L),  // 10^12
			Fixed.fromLong(10000000000000L),  // 10^13
			Fixed.fromLong(100000000000000L),  // 10^14
			Fixed.fromLong(1000000000000000L),  // 10^15
			Fixed.fromLong(10000000000000000L),  // 10^16
			Fixed.fromLong(100000000000000000L),  // 10^17
			Fixed.fromLong(1000000000000000000L),  // 10^18
	};
	
	public float floatValue() {
        return (float)_data / ONE;
	}
	
	public int intValue() {
        return _data >> FRACTIONAL_PLACES;
	}
	
	public int dataValue() {
		return _data;
	}
}
