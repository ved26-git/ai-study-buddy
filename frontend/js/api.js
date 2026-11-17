
/**
 * AI Study Buddy - REST API Client
 * Frontend: HTML5 / Bootstrap 5 / Vanilla JavaScript
 * Backend: Java 17 / Spring Boot
 *
 * Live mode is the default.
 * Live API errors are never silently replaced with mock data.
 * Demo mode is explicitly selected by the user.
 */

const API_CONFIG = {
    baseUrl: 'http://localhost:8080/api',
    tokenKey: 'ai_study_buddy_jwt',
    userKey: 'ai_study_buddy_user',
    modeKey: 'ai_study_buddy_mode',
};

class ApiService {
    constructor() {
        this.token = localStorage.getItem(API_CONFIG.tokenKey) || null;
        this.currentUser = this.readStoredUser();
        this.mode = localStorage.getItem(API_CONFIG.modeKey) === 'demo'
            ? 'demo'
            : 'live';

        // Initialize mock data only when Demo mode is selected.
        if (this.mode === 'demo') {
            this.initMockDb();
        }
    }

    readStoredUser() {
        try {
            return JSON.parse(localStorage.getItem(API_CONFIG.userKey) || 'null');
        } catch (error) {
            console.error('Invalid stored user data:', error);
            localStorage.removeItem(API_CONFIG.userKey);
            return null;
        }
    }

    setMode(mode) {
        this.mode = mode === 'demo' ? 'demo' : 'live';
        localStorage.setItem(API_CONFIG.modeKey, this.mode);

        if (this.mode === 'demo') {
            this.initMockDb();
        }
    }

    getMode() {
        return this.mode;
    }

    isLive() {
        return this.mode === 'live';
    }

    getToken() {
        return this.token;
    }

    setToken(token) {
        this.token = token || null;

        if (this.token) {
            localStorage.setItem(API_CONFIG.tokenKey, this.token);
        } else {
            localStorage.removeItem(API_CONFIG.tokenKey);
        }
    }

    setCurrentUser(user) {
        this.currentUser = user || null;

        if (this.currentUser) {
            localStorage.setItem(
                API_CONFIG.userKey,
                JSON.stringify(this.currentUser)
            );
        } else {
            localStorage.removeItem(API_CONFIG.userKey);
        }
    }

    clearSession() {
        this.setToken(null);
        this.setCurrentUser(null);
    }

    getCurrentUser() {
        return this.currentUser;
    }

    // ==========================================
    // COMMON HTTP REQUEST HANDLER
    // ==========================================

    async request(endpoint, options = {}) {
        if (!endpoint.startsWith('/')) {
            throw new Error('API endpoint must start with /.');
        }

        if (this.mode === 'demo') {
            return this.handleFallback(endpoint, options);
        }

        const {
            skipAuthHandling = false,
            ...fetchOptions
        } = options;

        const headers = {
            Accept: 'application/json',
            ...(fetchOptions.body !== undefined
                ? { 'Content-Type': 'application/json' }
                : {}),
            ...(fetchOptions.headers || {}),
        };

        if (this.token) {
            headers.Authorization = `Bearer ${this.token}`;
        }

        let response;

        try {
            response = await fetch(
                `${API_CONFIG.baseUrl}${endpoint}`,
                {
                    ...fetchOptions,
                    headers,
                }
            );
        } catch (error) {
            console.error(`Network error [${endpoint}]:`, error);

            throw new Error(
                `Unable to connect to the Spring Boot backend at ${API_CONFIG.baseUrl}. ` +
                'Check that the backend is running on port 8080 and MySQL is active.'
            );
        }

        // Clear the local session directly.
        // Do NOT call logout() here: that would create another request
        // and could cause an infinite 401/logout loop.
        if (response.status === 401 && !skipAuthHandling) {
            this.clearSession();

            throw new Error(
                'Your session has expired or is invalid. Please sign in again.'
            );
        }

        if (!response.ok) {
            let errorData = {};

            try {
                const text = await response.text();
                errorData = text ? JSON.parse(text) : {};
            } catch {
                errorData = {};
            }

            const message =
                errorData.error ||
                errorData.message ||
                `Server responded with HTTP ${response.status}`;

            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        const responseText = await response.text();

        if (!responseText) {
            return null;
        }

        try {
            return JSON.parse(responseText);
        } catch {
            throw new Error(
                `The backend returned an invalid JSON response for ${endpoint}.`
            );
        }
    }

    // ==========================================
    // AUTHENTICATION & USER PROFILE
    // ==========================================

    async register(name, email, password, profilePicture = null) {
        if (!this.isLive()) {
            return this.mockRegister(name, email, password, profilePicture);
        }

        const result = await this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify({
                name,
                email,
                password,
                profilePicture,
            }),
        });

        if (!result?.token || !result?.user) {
            throw new Error('Invalid registration response from backend.');
        }

        this.setToken(result.token);
        this.setCurrentUser(result.user);

        return result;
    }

    async login(email, password) {
        if (!this.isLive()) {
            return this.mockLogin(email, password);
        }

        const result = await this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password }),
        });

        if (!result?.token || !result?.user) {
            throw new Error('Invalid login response from backend.');
        }

        this.setToken(result.token);
        this.setCurrentUser(result.user);

        return result;
    }

    async getMe() {
        if (!this.isLive()) {
            if (!this.currentUser) {
                throw new Error('Please sign in first.');
            }

            return this.currentUser;
        }

        const user = await this.request('/auth/me', {
            method: 'GET',
        });

        this.setCurrentUser(user);
        return user;
    }

    async updateProfile(name, profilePicture) {
        if (this.isLive()) {
            const updatedUser = await this.request('/auth/profile', {
                method: 'PUT',
                body: JSON.stringify({ name, profilePicture }),
            });

            this.setCurrentUser(updatedUser);
            return updatedUser;
        }

        if (!this.currentUser) {
            throw new Error('Please sign in first.');
        }

        if (name) {
            this.currentUser.name = name;
        }

        if (profilePicture) {
            this.currentUser.profilePicture = profilePicture;
            this.currentUser.profile_picture = profilePicture;
        }

        this.setCurrentUser(this.currentUser);

        const users = this.readMockData('asb_mock_users');

        const index = users.findIndex(
            user =>
                user.userId === this.currentUser.userId ||
                user.email === this.currentUser.email
        );

        if (index !== -1) {
            users[index] = {
                ...users[index],
                name: this.currentUser.name,
                profilePicture: this.currentUser.profilePicture,
            };

            this.saveMockData('asb_mock_users', users);
        }

        return this.currentUser;
    }

    async logout() {
        // Attempt server-side logout before clearing local credentials.
        // skipAuthHandling prevents recursive 401 handling.
        if (this.isLive() && this.token) {
            try {
                await this.request('/auth/logout', {
                    method: 'POST',
                    skipAuthHandling: true,
                });
            } catch (error) {
                console.warn('Server logout request failed:', error.message);
            }
        }

        this.clearSession();
        return { message: 'Logged out successfully.' };
    }

    // ==========================================
    // NOTES SUMMARIZER
    // ==========================================

    async summarizeNotes(title, content) {
        return this.request('/notes/summarize', {
            method: 'POST',
            body: JSON.stringify({ title, content }),
        });
    }

    async getNotes() {
        return this.request('/notes', { method: 'GET' });
    }

    async getNoteById(id) {
        return this.request(`/notes/${encodeURIComponent(id)}`, {
            method: 'GET',
        });
    }

    async deleteNote(id) {
        return this.request(`/notes/${encodeURIComponent(id)}`, {
            method: 'DELETE',
        });
    }

    // ==========================================
    // QUIZ GENERATOR & SCORING
    // ==========================================

    async generateQuiz(topic, numQuestions, difficulty) {
        return this.request('/quiz/generate', {
            method: 'POST',
            body: JSON.stringify({
                topic,
                numQuestions,
                difficulty,
            }),
        });
    }

    async submitQuiz(quizId, answers) {
        return this.request(
            `/quiz/${encodeURIComponent(quizId)}/submit`,
            {
                method: 'POST',
                body: JSON.stringify({ answers }),
            }
        );
    }

    async getQuizzes() {
        return this.request('/quiz', { method: 'GET' });
    }

    async getQuizById(id) {
        return this.request(`/quiz/${encodeURIComponent(id)}`, {
            method: 'GET',
        });
    }

    async deleteQuiz(id) {
        return this.request(`/quiz/${encodeURIComponent(id)}`, {
            method: 'DELETE',
        });
    }

    // ==========================================
    // AI DOUBT SOLVER
    // ==========================================

    async askDoubt(question) {
        return this.request('/doubts/ask', {
            method: 'POST',
            body: JSON.stringify({ question }),
        });
    }

    async getDoubtHistory() {
        return this.request('/doubts/history', {
            method: 'GET',
        });
    }

    async deleteDoubt(id) {
        return this.request(`/doubts/${encodeURIComponent(id)}`, {
            method: 'DELETE',
        });
    }

    async clearDoubts() {
        return this.request('/doubts/clear', {
            method: 'DELETE',
        });
    }

    // ==========================================
    // DASHBOARD & HISTORY
    // ==========================================

    async getDashboardStats() {
        return this.request('/dashboard/stats', {
            method: 'GET',
        });
    }

    async getRecentActivities() {
        return this.request('/dashboard/recent-activities', {
            method: 'GET',
        });
    }

    async getHistory() {
        return this.request('/history', {
            method: 'GET',
        });
    }

    async getAppConfig() {
        // Do not hide Live backend errors with mock configuration.
        if (this.isLive()) {
            return this.request('/config', {
                method: 'GET',
            });
        }

        return {
            geminiModel: 'gemini-3.8-flash',
            appName: 'AI Study Buddy',
        };
    }

    // ==========================================
    // DEMO DATA HELPERS
    // ==========================================

    readMockData(key) {
        try {
            return JSON.parse(localStorage.getItem(key) || '[]');
        } catch (error) {
            console.error(`Invalid Demo data: ${key}`, error);
            return [];
        }
    }

    saveMockData(key, data) {
        localStorage.setItem(key, JSON.stringify(data));
    }

    initMockDb() {
        const avatar =
            '/assets/images/student_avatar_user_1794203529926.jpg';

        if (!localStorage.getItem('asb_mock_users')) {
            this.saveMockData('asb_mock_users', [
                {
                    userId: 1,
                    name: 'Alex Johnson',
                    email: 'alex@aistudybuddy.com',
                    password: 'password123',
                    profilePicture: avatar,
                    createdAt: new Date(
                        Date.now() - 7 * 86400000
                    ).toISOString(),
                },
            ]);
        }

        if (!localStorage.getItem('asb_mock_notes')) {
            this.saveMockData('asb_mock_notes', [
                {
                    noteId: 1,
                    userId: 1,
                    title: 'Operating Systems: Deadlock',
                    originalContent:
                        'Deadlock occurs when processes wait for resources held by one another.',
                    summary:
                        '### Deadlock\n\nDeadlock occurs when processes cannot proceed because each process is waiting for a resource held by another process.\n\n' +
                        '### Four Coffman Conditions\n\n' +
                        '1. Mutual Exclusion\n' +
                        '2. Hold and Wait\n' +
                        '3. No Preemption\n' +
                        '4. Circular Wait\n\n' +
                        'Prevention requires breaking at least one necessary condition.',
                    createdAt: new Date(
                        Date.now() - 2 * 86400000
                    ).toISOString(),
                },
            ]);
        }

        if (!localStorage.getItem('asb_mock_quizzes')) {
            this.saveMockData('asb_mock_quizzes', [
                {
                    quizId: 1,
                    userId: 1,
                    topic: 'Java OOP',
                    difficulty: 'Medium',
                    totalQuestions: 3,
                    score: 3,
                    questions: [
                        {
                            question:
                                'Which keyword prevents method overriding in Java?',
                            options: ['static', 'final', 'abstract', 'const'],
                            correctAnswer: 'final',
                            explanation:
                                'The final keyword prevents a method from being overridden.',
                        },
                        {
                            question:
                                'Which interface supports natural ordering?',
                            options: [
                                'Serializable',
                                'Cloneable',
                                'Comparable',
                                'Iterable',
                            ],
                            correctAnswer: 'Comparable',
                            explanation:
                                'Comparable defines compareTo() for natural ordering.',
                        },
                        {
                            question:
                                'Which OOP principle hides internal implementation?',
                            options: [
                                'Abstraction',
                                'Compilation',
                                'Iteration',
                                'Execution',
                            ],
                            correctAnswer: 'Abstraction',
                            explanation:
                                'Abstraction exposes essential behavior while hiding implementation details.',
                        },
                    ],
                    submittedAnswers: {
                        0: 'final',
                        1: 'Comparable',
                        2: 'Abstraction',
                    },
                    createdAt: new Date(
                        Date.now() - 86400000
                    ).toISOString(),
                    completedAt: new Date(
                        Date.now() - 86400000
                    ).toISOString(),
                },
            ]);
        }

        if (!localStorage.getItem('asb_mock_doubts')) {
            this.saveMockData('asb_mock_doubts', [
                {
                    doubtId: 1,
                    userId: 1,
                    question:
                        'What is the difference between Comparable and Comparator?',
                    answer:
                        '### Comparable vs Comparator\n\n' +
                        '**Comparable** defines natural ordering through compareTo().\n\n' +
                        '**Comparator** defines custom ordering through compare().',
                    createdAt: new Date(
                        Date.now() - 3 * 86400000
                    ).toISOString(),
                },
            ]);
        }
    }

    // ==========================================
    // DEMO API ROUTER
    // ==========================================

    handleFallback(endpoint, options = {}) {
        const method = (options.method || 'GET').toUpperCase();

        let body = {};

        try {
            body = options.body ? JSON.parse(options.body) : {};
        } catch {
            throw new Error('Invalid JSON request body in Demo mode.');
        }

        const notes = this.readMockData('asb_mock_notes');
        const quizzes = this.readMockData('asb_mock_quizzes');
        const doubts = this.readMockData('asb_mock_doubts');

        const currentUserId = this.currentUser?.userId || 1;

        const getId = value => {
            const id = Number(value);
            return Number.isFinite(id) ? id : null;
        };

        // AUTHENTICATION

        if (endpoint === '/auth/me' && method === 'GET') {
            if (!this.currentUser) {
                throw new Error('Please sign in first.');
            }

            return this.currentUser;
        }

        if (endpoint === '/auth/logout' && method === 'POST') {
            return { message: 'Logged out successfully.' };
        }

        if (endpoint === '/auth/profile' && method === 'PUT') {
            return this.updateProfile(body.name, body.profilePicture);
        }

        // APP CONFIGURATION

        if (endpoint === '/config' && method === 'GET') {
            return {
                appName: 'AI Study Buddy',
                geminiModel: 'gemini-3.8-flash',
            };
        }

        // NOTES

        if (endpoint === '/notes/summarize' && method === 'POST') {
            if (!body.content || !body.content.trim()) {
                throw new Error('Please enter note content.');
            }

            const title = body.title?.trim() || 'Study Notes Summary';

            const newNote = {
                noteId: Date.now(),
                userId: currentUserId,
                title,
                originalContent: body.content,
                summary:
                    `### ${title}\n\n` +
                    'This is a Demo-mode sample summary. It does not use Gemini AI.\n\n' +
                    'Review the original content and identify its important definitions, concepts, examples, and key points.',
                createdAt: new Date().toISOString(),
            };

            notes.unshift(newNote);
            this.saveMockData('asb_mock_notes', notes);

            return newNote;
        }

        if (endpoint === '/notes' && method === 'GET') {
            return notes.filter(note => note.userId === currentUserId);
        }

        const noteMatch = endpoint.match(/^\/notes\/([^/]+)$/);

        if (noteMatch) {
            const id = getId(decodeURIComponent(noteMatch[1]));
            const note = notes.find(
                item =>
                    item.noteId === id &&
                    item.userId === currentUserId
            );

            if (method === 'GET') {
                if (!note) throw new Error('Note not found.');
                return note;
            }

            if (method === 'DELETE') {
                if (!note) throw new Error('Note not found.');

                this.saveMockData(
                    'asb_mock_notes',
                    notes.filter(item => item.noteId !== id)
                );

                return { message: 'Note deleted successfully.' };
            }
        }

        // QUIZ GENERATION

        if (endpoint === '/quiz/generate' && method === 'POST') {
            const topic = body.topic?.trim();
            const count = Number(body.numQuestions || 5);
            const difficulty = body.difficulty || 'Medium';

            if (!topic) {
                throw new Error('Please enter a quiz topic.');
            }

            if (!Number.isInteger(count) || count < 1 || count > 20) {
                throw new Error('Question count must be between 1 and 20.');
            }

            const questions = [];

            for (let i = 0; i < count; i++) {
                questions.push({
                    index: i,
                    question:
                        `Question ${i + 1}: Which approach is useful when studying ${topic}?`,
                    options: [
                        'Understand concepts and practise examples',
                        'Ignore definitions and skip practice',
                        'Memorize without understanding',
                        'Avoid reviewing mistakes',
                    ],
                    correctAnswer:
                        'Understand concepts and practise examples',
                    explanation:
                        'Understanding concepts and practising examples helps build knowledge.',
                });
            }

            const newQuiz = {
                quizId: Date.now(),
                userId: currentUserId,
                topic,
                difficulty,
                totalQuestions: count,
                score: null,
                questions,
                createdAt: new Date().toISOString(),
                completedAt: null,
            };

            quizzes.unshift(newQuiz);
            this.saveMockData('asb_mock_quizzes', quizzes);

            // Hide answers until the quiz is submitted.
            return {
                ...newQuiz,
                questions: questions.map(
                    ({ correctAnswer, explanation, ...question }) => question
                ),
            };
        }

        if (endpoint === '/quiz' && method === 'GET') {
            return quizzes
                .filter(quiz => quiz.userId === currentUserId)
                .map(quiz => ({
                    quizId: quiz.quizId,
                    topic: quiz.topic,
                    difficulty: quiz.difficulty,
                    totalQuestions: quiz.totalQuestions,
                    score: quiz.score,
                    createdAt: quiz.createdAt,
                    completedAt: quiz.completedAt,
                }));
        }

        // QUIZ SUBMISSION AND INDIVIDUAL QUIZ

        const submitMatch = endpoint.match(
            /^\/quiz\/([^/]+)\/submit$/
        );

        if (submitMatch && method === 'POST') {
            const id = getId(decodeURIComponent(submitMatch[1]));

            const quiz = quizzes.find(
                item =>
                    item.quizId === id &&
                    item.userId === currentUserId
            );

            if (!quiz) {
                throw new Error('Quiz not found.');
            }

            if (quiz.completedAt || quiz.score !== null) {
                throw new Error('This quiz has already been submitted.');
            }

            if (
                !body.answers ||
                typeof body.answers !== 'object' ||
                Array.isArray(body.answers)
            ) {
                throw new Error('Invalid quiz answers.');
            }

            const questions = quiz.questions;

            for (const key of Object.keys(body.answers)) {
                const index = Number(key);

                if (
                    !Number.isInteger(index) ||
                    index < 0 ||
                    index >= questions.length
                ) {
                    throw new Error('Invalid answer question index.');
                }

                const answer = body.answers[key];

                if (
                    typeof answer !== 'string' ||
                    !questions[index].options.includes(answer)
                ) {
                    throw new Error(`Invalid answer for question ${index + 1}.`);
                }
            }

            let score = 0;

            questions.forEach((question, index) => {
                if (
                    body.answers[index] === question.correctAnswer
                ) {
                    score++;
                }
            });

            quiz.score = score;
            quiz.submittedAnswers = body.answers;
            quiz.completedAt = new Date().toISOString();

            this.saveMockData('asb_mock_quizzes', quizzes);

            return {
                quizId: quiz.quizId,
                score,
                totalQuestions: quiz.totalQuestions,
                percentage: Math.round(
                    (score / quiz.totalQuestions) * 100
                ),
                questions,
                submittedAnswers: body.answers,
            };
        }

        const quizMatch = endpoint.match(/^\/quiz\/([^/]+)$/);

        if (quizMatch) {
            const id = getId(decodeURIComponent(quizMatch[1]));

            const quiz = quizzes.find(
                item =>
                    item.quizId === id &&
                    item.userId === currentUserId
            );

            if (method === 'GET') {
                if (!quiz) throw new Error('Quiz not found.');

                if (quiz.completedAt) {
                    return quiz;
                }

                return {
                    ...quiz,
                    questions: quiz.questions.map(
                        ({ correctAnswer, explanation, ...question }) =>
                            question
                    ),
                };
            }

            if (method === 'DELETE') {
                if (!quiz) throw new Error('Quiz not found.');

                this.saveMockData(
                    'asb_mock_quizzes',
                    quizzes.filter(item => item.quizId !== id)
                );

                return { message: 'Quiz deleted successfully.' };
            }
        }

        // DOUBTS

        if (endpoint === '/doubts/ask' && method === 'POST') {
            if (!body.question || !body.question.trim()) {
                throw new Error('Please enter your question.');
            }

            const newDoubt = {
                doubtId: Date.now(),
                userId: currentUserId,
                question: body.question,
                answer:
                    `### Explanation\n\n` +
                    `You asked: ${body.question}\n\n` +
                    'This is a Demo-mode sample response. It is not generated by Gemini AI.',
                createdAt: new Date().toISOString(),
            };

            doubts.unshift(newDoubt);
            this.saveMockData('asb_mock_doubts', doubts);

            return newDoubt;
        }

        if (endpoint === '/doubts/history' && method === 'GET') {
            return doubts.filter(
                doubt => doubt.userId === currentUserId
            );
        }

        if (endpoint === '/doubts/clear' && method === 'DELETE') {
            this.saveMockData(
                'asb_mock_doubts',
                doubts.filter(doubt => doubt.userId !== currentUserId)
            );

            return { message: 'All doubts cleared.' };
        }

        const doubtMatch = endpoint.match(/^\/doubts\/([^/]+)$/);

        if (doubtMatch && method === 'DELETE') {
            const id = getId(decodeURIComponent(doubtMatch[1]));

            const doubt = doubts.find(
                item =>
                    item.doubtId === id &&
                    item.userId === currentUserId
            );

            if (!doubt) {
                throw new Error('Doubt not found.');
            }

            this.saveMockData(
                'asb_mock_doubts',
                doubts.filter(item => item.doubtId !== id)
            );

            return { message: 'Doubt deleted successfully.' };
        }

        // DASHBOARD STATISTICS

        if (endpoint === '/dashboard/stats' && method === 'GET') {
            const userNotes = notes.filter(
                item => item.userId === currentUserId
            );

            const userQuizzes = quizzes.filter(
                item => item.userId === currentUserId
            );

            const userDoubts = doubts.filter(
                item => item.userId === currentUserId
            );

            const completed = userQuizzes.filter(
                quiz =>
                    quiz.score !== null &&
                    quiz.totalQuestions > 0
            );

            const average = completed.length
                ? completed.reduce(
                    (sum, quiz) =>
                        sum + (quiz.score / quiz.totalQuestions) * 100,
                    0
                ) / completed.length
                : 0;

            return {
                totalSummaries: userNotes.length,
                totalQuizzes: completed.length,
                averageQuizScore: Number.isFinite(average)
                    ? Math.round(average)
                    : 0,
                totalDoubts: userDoubts.length,
            };
        }

        // RECENT ACTIVITIES

        if (
            endpoint === '/dashboard/recent-activities' &&
            method === 'GET'
        ) {
            const activities = [];

            notes
                .filter(item => item.userId === currentUserId)
                .forEach(note => {
                    activities.push({
                        type: 'NOTE',
                        id: note.noteId,
                        title: note.title,
                        detail: 'Generated Study Notes Summary',
                        timestamp: note.createdAt,
                    });
                });

            quizzes
                .filter(item => item.userId === currentUserId)
                .forEach(quiz => {
                    activities.push({
                        type: 'QUIZ',
                        id: quiz.quizId,
                        title: quiz.topic,
                        detail: quiz.score !== null
                            ? `Score: ${quiz.score}/${quiz.totalQuestions}`
                            : 'Quiz Generated',
                        timestamp: quiz.createdAt,
                    });
                });

            doubts
                .filter(item => item.userId === currentUserId)
                .forEach(doubt => {
                    activities.push({
                        type: 'DOUBT',
                        id: doubt.doubtId,
                        title: doubt.question,
                        detail: 'AI Tutor Doubt Resolved',
                        timestamp: doubt.createdAt,
                    });
                });

            return activities
                .sort(
                    (a, b) =>
                        new Date(b.timestamp) - new Date(a.timestamp)
                )
                .slice(0, 10);
        }

        // COMBINED HISTORY

        if (endpoint === '/history' && method === 'GET') {
            return {
                summaries: notes.filter(
                    item => item.userId === currentUserId
                ),
                quizzes: quizzes
                    .filter(item => item.userId === currentUserId)
                    .map(quiz => ({
                        quizId: quiz.quizId,
                        topic: quiz.topic,
                        difficulty: quiz.difficulty,
                        totalQuestions: quiz.totalQuestions,
                        score: quiz.score,
                        createdAt: quiz.createdAt,
                        completedAt: quiz.completedAt,
                    })),
                doubts: doubts.filter(
                    item => item.userId === currentUserId
                ),
            };
        }

        // Never silently hide an unsupported Demo endpoint.
        throw new Error(
            `Demo mode does not support ${method} ${endpoint}.`
        );
    }

    // ==========================================
    // DEMO AUTHENTICATION
    // ==========================================

    mockRegister(name, email, password, profilePicture = null) {
        if (!name?.trim() || !email?.trim() || !password) {
            throw new Error('Name, email and password are required.');
        }

        const users = this.readMockData('asb_mock_users');

        const existing = users.find(
            user =>
                user.email.toLowerCase() === email.toLowerCase()
        );

        if (existing) {
            throw new Error('An account with this email already exists.');
        }

        const newUser = {
            userId: Date.now(),
            name: name.trim(),
            email: email.trim().toLowerCase(),
            profilePicture: profilePicture ||
                '/assets/images/student_avatar_user_1794203529926.jpg',
            createdAt: new Date().toISOString(),
        };

        users.push({
            ...newUser,
            password,
        });

        this.saveMockData('asb_mock_users', users);

        const token =
            'jwt_mock_' + Math.random().toString(36).substring(2);

        this.setToken(token);
        this.setCurrentUser(newUser);

        return {
            message: 'Demo registration successful.',
            token,
            user: newUser,
        };
    }

    mockLogin(email, password) {
        const users = this.readMockData('asb_mock_users');

        const user = users.find(
            item =>
                item.email.toLowerCase() === email.toLowerCase() &&
                item.password === password
        );

        if (!user) {
            throw new Error('Invalid email or password.');
        }

        const safeUser = {
            userId: user.userId,
            name: user.name,
            email: user.email,
            profilePicture: user.profilePicture,
            createdAt: user.createdAt,
        };

        const token =
            'jwt_mock_' + Math.random().toString(36).substring(2);

        this.setToken(token);
        this.setCurrentUser(safeUser);

        return {
            message: 'Demo login successful.',
            token,
            user: safeUser,
        };
    }
}

// Create one shared API client.
window.apiService = new ApiService();
