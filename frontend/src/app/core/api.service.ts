import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export type UserRole = 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';

export interface LmsUser {
	id: number;
	name: string;
	email: string;
	role: UserRole;
	active: boolean;
}

interface AuthResponse {
	token: string;
	user: LmsUser;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
	private readonly base = 'http://localhost:8080/api';
	private readonly sessionKey = 'gu_lms.session';
	private readonly legacySessionKey = 'learnspace.session';
	readonly user = signal<LmsUser | null>(this.restoreUser());

	constructor(private readonly http: HttpClient) {}

	login(credentials: { email: string; password: string }): Observable<AuthResponse> {
		return this.http.post<AuthResponse>(`${this.base}/auth/login`, credentials, { withCredentials: true })
			.pipe(tap((response) => this.saveSession(response)));
	}

	register(details: { name: string; email: string; password: string }): Observable<AuthResponse> {
		return this.http.post<AuthResponse>(`${this.base}/auth/register`, details, { withCredentials: true })
			.pipe(tap((response) => this.saveSession(response)));
	}

	logout(): void {
		this.http.post(`${this.base}/auth/logout`, {}, this.options()).subscribe({ error: () => undefined });
		localStorage.removeItem(this.sessionKey);
		localStorage.removeItem(this.legacySessionKey);
		this.user.set(null);
	}

	updateProfile(id: number, changes: { name: string; email: string }): Observable<LmsUser> {
		return this.put<LmsUser>(`/users/${id}`, changes).pipe(
			tap((user) => {
				const session = localStorage.getItem(this.sessionKey) ?? localStorage.getItem(this.legacySessionKey);
				if (session) {
					const authResponse = JSON.parse(session) as AuthResponse;
					authResponse.user = user;
					localStorage.setItem(this.sessionKey, JSON.stringify(authResponse));
					localStorage.removeItem(this.legacySessionKey);
				}
				this.user.set(user);
			})
		);
	}

	get<T>(path: string): Observable<T> { return this.http.get<T>(this.base + path, this.options()); }
	post<T>(path: string, body: unknown): Observable<T> { return this.http.post<T>(this.base + path, body, this.options()); }
	put<T>(path: string, body: unknown): Observable<T> { return this.http.put<T>(this.base + path, body, this.options()); }
	patch<T>(path: string, body: unknown): Observable<T> { return this.http.patch<T>(this.base + path, body, this.options()); }
	delete<T>(path: string): Observable<T> { return this.http.delete<T>(this.base + path, this.options()); }

	private options(): { headers: HttpHeaders; withCredentials: boolean } {
		try {
			const session = localStorage.getItem(this.sessionKey) ?? localStorage.getItem(this.legacySessionKey);
			const token = session ? (JSON.parse(session) as AuthResponse).token : '';
			return { headers: token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders(), withCredentials: true };
		} catch { return { headers: new HttpHeaders(), withCredentials: true }; }
	}

	private restoreUser(): LmsUser | null {
		try {
			const session = localStorage.getItem(this.sessionKey) ?? localStorage.getItem(this.legacySessionKey);
			if (!session) return null;
			const response = JSON.parse(session) as AuthResponse;
			const oldDemoEmails: Record<string, string> = {
				'admin@learnspace.com': 'admin@gu-lms.com',
				'instructor@learnspace.com': 'instructor@gu-lms.com',
				'student@learnspace.com': 'student@gu-lms.com'
			};
			response.user.email = oldDemoEmails[response.user.email] ?? response.user.email;
			localStorage.setItem(this.sessionKey, JSON.stringify(response));
			localStorage.removeItem(this.legacySessionKey);
			return response.user;
		} catch {
			return null;
		}
	}

	private saveSession(response: AuthResponse): void {
		localStorage.setItem(this.sessionKey, JSON.stringify(response));
		localStorage.removeItem(this.legacySessionKey);
		this.user.set(response.user);
	}
}
