package com.mcdevon.fixed;

import java.math.BigDecimal;

public final class Fixed {
	private final int _data;
	
	// Static values for 32-bit fixed-point value
	private static final int MAX_VALUE = Integer.MAX_VALUE;
	private static final int MIN_VALUE = Integer.MIN_VALUE;
	
	public static final int BITS = 32;
	
	// NOTE: Decimal bits must be less than or equal to (BITS / 2) for mul() to work
	// and an even number for sqrt() to work!
	private static final int DECIMAL_BITS = 10;
	private static final int DECIMAL_MASK = new Integer(-1) >>> (BITS - DECIMAL_BITS);
	private static final int FULL_MASK = new Integer(-1);
	
	private static final int ONE = 1 << DECIMAL_BITS;
	private static final int TWO = 1 << (DECIMAL_BITS + 1);
	private static final int HALF = 1 << (DECIMAL_BITS - 1);
	
	public static final Fixed maxValue = new Fixed(MAX_VALUE);
	public static final Fixed minValue = new Fixed(MIN_VALUE);
	public static final Fixed zero = new Fixed(0);
	public static final Fixed one = new Fixed(ONE);
	public static final Fixed two = new Fixed(TWO);
	public static final Fixed half = new Fixed(HALF);
	public static final Fixed precision = new Fixed(1);
	
	// String part lengths
	//private static final int _leftLength = Integer.toString(maxValue.intValue()).length();
	private static final int _rightLength;
	static {
		String[] parts = precision.bigDecimalValue().toString().split("\\.");
		if (parts.length <= 1) {
			_rightLength = 0;
		} else {
			int res = 1;
			for (int i = 0; i < parts[1].length(); i++) {
				if (parts[1].charAt(i) == '0') {
					res++;
					continue;
				}
				break;
			}
			_rightLength = res;
		}
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
	
	public static final Fixed pi = Fixed.fromString("3.14159265358979323846264338327950288419716939937510");
	public static final Fixed piTimesTwo = Fixed.fromString("6.28318530717958647692528676655900576839433879875020");
	public static final Fixed piOverTwo = Fixed.fromString("1.57079632679489661923132169163975144209858469968755");
	public static final Fixed piInv = Fixed.fromString("0.31830988618379067153776752674502872406891929148091");
	public static final Fixed piOverTwoInv = Fixed.fromString("0.63661977236758134307553505349005744813783858296183");
	
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

        int xlo = xl & DECIMAL_MASK;
        int xhi = xl >> DECIMAL_BITS;
        int ylo = yl & DECIMAL_MASK;
        int yhi = yl >> DECIMAL_BITS;

        int lolo = xlo * ylo;
        int lohi = xlo * yhi;
        int hilo = xhi * ylo;
        int hihi = xhi * yhi;

        int loResult = lolo >>> DECIMAL_BITS;
        int midResult1 = lohi;
        int midResult2 = hilo;
        int hiResult = hihi << DECIMAL_BITS;

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
        int bitPos = DECIMAL_BITS + 1;

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
	
	public boolean greaterThan(Fixed value) {
		return _data > value._data;
	}
	
	public boolean lessThanOrEquals(Fixed value) {
		return _data <= value._data;
	}
	
	public boolean greaterThanOrEquals(Fixed value) {
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
	
	public Fixed safeMul(Fixed value) {
        int xl = _data;
        int yl = value._data;

        int xlo = xl & DECIMAL_MASK;
        int xhi = xl >> DECIMAL_BITS;
        int ylo = yl & DECIMAL_MASK;
        int yhi = yl >> DECIMAL_BITS;

        int lolo = xlo * ylo;
        int lohi = xlo * yhi;
        int hilo = xhi * ylo;
        int hihi = xhi * yhi;

        int loResult = lolo >>> DECIMAL_BITS;
        int midResult1 = lohi;
        int midResult2 = hilo;
        int hiResult = hihi << DECIMAL_BITS;

        boolean overflow = false;
        
        int sum = loResult + midResult1;
        
        // a + b overflows if sign(x) ^ sign(y) != sign(sum),
        // test after every add
        overflow |= ((loResult ^ midResult1 ^ sum) & MIN_VALUE) != 0;
        
        int sumTest = sum + midResult2;
        overflow |= ((sum ^ midResult2 ^ sumTest) & MIN_VALUE) != 0;
        
        sum = sumTest + hiResult;
        overflow |= ((sumTest ^ hiResult ^ sum) & MIN_VALUE) != 0;

        boolean opSignsEqual = ((xl ^ yl) & MIN_VALUE) == 0;

        // check for same sign operands and negative result and for
        // different sign operands and positive result
        if (opSignsEqual) {
            if (sum < 0 || (overflow && xl > 0)) {
                return maxValue;
            }
        }
        else if (sum > 0) {
            return minValue;
        }

        // if the top bits of hihi are neither all 0s or 1s,
        // then this means the result overflowed.
        int topBits = hihi >> DECIMAL_BITS;
        if (topBits != 0 && topBits != -1) {
            return opSignsEqual ? maxValue : minValue; 
        }

        // Last case of negative overflow
        if (!opSignsEqual) {
            int posOp;
            int negOp;
            if (xl > yl) {
                posOp = xl;
                negOp = yl;
            }
            else {
                posOp = yl;
                negOp = xl;
            }
            if (sum > negOp && negOp < -ONE && posOp > ONE) {
                return minValue;
            }
        }

        return new Fixed(sum);
	}
	
	public Fixed safeMod(Fixed value) {
		return new Fixed(_data == MIN_VALUE & value._data == -1 ? 0 :
                		 _data % value._data);
	}
	
	/*
	 * Math operations
	 */
		
	public static Fixed sqrt(Fixed x) {
		// BitShift-based sqrt
		
        int xl = x._data;
        if (xl < 0) {
            // Sqrt not defined for negative numbers and NaN not available
            throw new ArithmeticException("Sqrt for negative number");
        }

        int num = xl;
        int result = 0;

        // second-highest bit
        int bit = 1 << (BITS - 2);

        while (bit > num) {
            bit >>>= 2;
        }

        for (int i = 0; i < 2; i++) {
            // First we get the top bits of the answer.
            while (bit != 0) {
                if (num >= result + bit) {
                    num -= result + bit;
                    result = (result >>> 1) + bit;
                }
                else {
                    result = result >>> 1;
                }
                bit >>>= 2;
            }

            if (i == 0) {
                // Then process it again to get the lowest 16 bits.
                if (num > (1 << (DECIMAL_BITS)) - 1) {
                    // The remainder 'num' is too large to be shifted left
                    // by 32, so we have to add 1 to result manually and
                    // adjust 'num' accordingly.
                    // num = a - (result + 0.5)^2
                    //       = num + result^2 - (result + 0.5)^2
                    //       = num - result - 0.5
                    num -= result;
                    num = (num << (DECIMAL_BITS)) - HALF;
                    result = (result << (DECIMAL_BITS)) + HALF;
                }
                else {
                    num <<= (DECIMAL_BITS);
                    result <<= (DECIMAL_BITS);
                }

                bit = 1 << (DECIMAL_BITS - 2);
            }
        }
        // Finally, if next bit would have been 1, round the result upwards.
        if (num > result) {
            ++result;
        }
        return new Fixed(result);
    }
	
	/*
	 * Compatibility
	 */
	
	public static Fixed fromData(int dataValue) {
		return new Fixed(dataValue);
	}
	
	public static Fixed fromInt(int value) {
		return new Fixed(value * ONE);
	}
	
	public static Fixed fromLong(long value) {
		return new Fixed((int)value * ONE);
	}
	
	public static Fixed fromFloat(float value) {
		return new Fixed((int)(value * ONE));
	}
	
	public static Fixed fromDouble(double value) {
		return new Fixed((int)(value * ONE));
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

		if (left > maxValue.intValue()) {
			throw new NumberFormatException("Invalid input string: too large number");
		}
		
		if (parts.length == 2) {

			if (parts[1].length() > _rightLength) {
				parts[1] = parts[1].substring(0, _rightLength);
			}
			
			int right = Integer.parseInt(parts[1]);
			Fixed divider = _tenPowerTable[parts[1].length()]; // 10 ^parts.Length
			
			// Decimal precision of fromString may decrease when DECIMAL_BITS == 16
			while (divider.intValue() < 0) {
				parts[1] = parts[1].substring(0, parts[1].length() - 1);
				right = Integer.parseInt(parts[1]);
				divider = _tenPowerTable[parts[1].length()];
			}
				
			Fixed fLeft = Fixed.fromInt(left);
			Fixed sign = fLeft.lessThan(Fixed.zero) ? Fixed.one.negate() : Fixed.one;

			Fixed lr = Fixed.fromInt(right);
			lr = lr.div(divider).mul(sign);
			
			return fLeft.add(lr);
		}

		return Fixed.fromInt(left);
	}
	
	public float floatValue() {
        return (float)_data / ONE;
	}
	
	public double doubleValue() {
        return (double)_data / ONE;
	}
	
	public int intValue() {
		return _data >> DECIMAL_BITS;
	}
	
	public BigDecimal bigDecimalValue() {
		return new BigDecimal(_data).divide(new BigDecimal(ONE));
	}
	
	public int dataValue() {
		return _data;
	}
	
	public String toString() {
		return bigDecimalValue().toString();
	}
}
