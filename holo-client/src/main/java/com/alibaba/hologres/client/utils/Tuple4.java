package com.alibaba.hologres.client.utils;

import java.util.Objects;

/**
 * 元组.
 *
 * @param <F0>
 * @param <F1>
 * @param <F2>
 * @param <F3>
 */
public class Tuple4<F0, F1, F2, F3> {
	public F0 f0;
	public F1 f1;
	public F2 f2;
	public F3 f3;

	public Tuple4(F0 f0, F1 f1, F2 f2, F3 f3) {
		this.f0 = f0;
		this.f1 = f1;
		this.f2 = f2;
		this.f3 = f3;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Tuple4<?, ?, ?, ?> tuple = (Tuple4<?, ?, ?, ?>) o;
		return Objects.equals(f0, tuple.f0) &&
				Objects.equals(f1, tuple.f1) &&
				Objects.equals(f2, tuple.f2) &&
				Objects.equals(f3, tuple.f3);
	}

	@Override
	public int hashCode() {
		return Objects.hash(f0, f1, f2, f3);
	}
}
