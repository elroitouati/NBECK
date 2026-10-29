import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import fs from 'fs';
import firebase from 'firebase/compat/app';
import 'firebase/compat/firestore';
const inc = (n) => firebase.firestore.FieldValue.increment(n);
const env = await initializeTestEnvironment({ projectId: 'dnba-test', firestore: { rules: fs.readFileSync('../../firestore.rules', 'utf8'), host: '127.0.0.1', port: 8085 } });
const A = env.authenticatedContext('alice').firestore();
const B = env.authenticatedContext('bob').firestore();
const anon = env.unauthenticatedContext().firestore();
let ok = 0, bad = 0;
async function t(name, p) { try { await p; console.log('PASS', name); ok++; } catch (e) { console.log('FAIL', name, e.message); bad++; } }
const id = { nickname: 'Alice', team: 2, photo: null };
const result = (won, guesses, seconds, points) => ({ ...id, won, guesses, seconds, points, rank: (won ? guesses : 9) * 1000000 + seconds });
function submit(db, uid, date, won, guesses, seconds, points = won ? 11 - guesses : 0, leaguePts = points) {
  const b = db.batch();
  b.set(db.doc(`daily/${date}/results/${uid}`), result(won, guesses, seconds, points));
  b.set(db.doc(`league/${date.slice(0, 7)}/players/${uid}`), { ...id, points: inc(leaguePts), played: inc(1), wins: inc(won ? 1 : 0), lastDate: date }, { merge: true });
  return b.commit();
}
// profiles & nicknames
await t('own profile', assertSucceeds(A.doc('users/alice').set({ nickname: 'Alice', team: 2 })));
await t('not someone else\'s profile', assertFails(A.doc('users/bob').set({ nickname: 'X1' })));
await t('signed-out read denied', assertFails(anon.doc('users/alice').get()));
await t('claim nickname', assertSucceeds(A.doc('nicknames/alice').set({ uid: 'alice' })));
await t('nickname already taken', assertFails(B.doc('nicknames/alice').set({ uid: 'bob' })));
// daily + league
await t('first daily of the day', assertSucceeds(submit(A, 'alice', '2026-10-29', true, 3, 95)));
const lg = (await A.doc('league/2026-10/players/alice').get()).data();
await t('league = 8 points, 1 played', lg.points === 8 && lg.played === 1 ? Promise.resolve() : Promise.reject(new Error(JSON.stringify(lg))));
await t('second submit same day rejected', assertFails(submit(A, 'alice', '2026-10-29', true, 1, 5)));
await t('next day adds up', assertSucceeds(submit(A, 'alice', '2026-10-30', false, 8, 300)));
const lg2 = (await A.doc('league/2026-10/players/alice').get()).data();
await t('league = 8 points, 2 played', lg2.points === 8 && lg2.played === 2 ? Promise.resolve() : Promise.reject(new Error(JSON.stringify(lg2))));
await t('wrong points in result rejected', assertFails(submit(A, 'alice', '2026-10-31', true, 2, 20, 10)));
await t('inflated league points rejected', assertFails(submit(A, 'alice', '2026-10-31', true, 2, 20, 9, 50)));
await t('fake rank rejected', assertFails(A.doc('daily/2026-10-31/results/alice').set({ ...result(true, 2, 20, 9), rank: 1 })));
await t('league without daily rejected', assertFails(A.doc('league/2026-10/players/alice').set({ points: inc(10), played: inc(1), lastDate: '2026-10-29' }, { merge: true })));
await t('league date must be in that month', assertFails(submit(A, 'alice', '2026-11-01', true, 2, 20).then(() => A.doc('league/2026-10/players/alice').set({ points: inc(9), played: inc(1), lastDate: '2026-11-01' }, { merge: true }))));
await t('rename only', assertSucceeds(A.doc('league/2026-10/players/alice').update({ nickname: 'Ali', team: 5, photo: null })));
await t('rename cannot touch points', assertFails(A.doc('league/2026-10/players/alice').update({ nickname: 'Ali', points: 99 })));
await t('cannot write bob\'s league', assertFails(submit(A, 'bob', '2026-10-29', true, 3, 95)));
await t('bob reads table', assertSucceeds(B.collection('daily/2026-10-29/results').orderBy('rank').get()));
console.log(`\n${ok} passed, ${bad} failed`);
await env.cleanup();
process.exit(bad ? 1 : 0);
