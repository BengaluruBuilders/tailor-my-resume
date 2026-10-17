import assert from 'node:assert/strict';
import { randomUUID, createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { writeFileSync, readFileSync } from 'node:fs';
import { activationFixture } from '/tmp/tmr-earn-20261017-backend02/test/tailormyresume-activation-fixture.ts';
import pg from '/tmp/tmr-earn-20261017-backend02/node_modules/pg/lib/index.js';
const { Pool } = pg;
import { drizzle } from '/tmp/tmr-earn-20261017-backend02/node_modules/drizzle-orm/node-postgres/index.js';
import { appConfigSchema } from '/tmp/tmr-earn-20261017-backend02/src/apps.ts';
import { deleteAccount, withLiveUser } from '/tmp/tmr-earn-20261017-backend02/src/core/accounts/service.ts';
const tableNames=['tailormyresume_google_activations','tailormyresume_upgrade_intents','tailormyresume_google_aliases','tailormyresume_activation_operations'];
const source=await activationFixture();
const adminUrl=process.env.TEST_DATABASE_URL!;
const admin=new Pool({connectionString:adminUrl,max:1});
const restoredName='test_slice02_restore_'+randomUUID().replaceAll('-','');
let restored:any;
const output='/home/abhishek-dubey/code/BengaluruBuilders/TailorMyResume/frontend/docs/testing/earn-credits/2026-10-17/02-google-signup/validation-evidence';
const archive='/tmp/tmr-slice02-candidate-schema.dump';
function pgEnv(url:string){ const u=new URL(url);return {...process.env,PGHOST:u.hostname,PGPORT:u.port,PGUSER:decodeURIComponent(u.username),PGPASSWORD:decodeURIComponent(u.password),PGDATABASE:u.pathname.slice(1),PGSSLMODE:'disable'};}
async function snapshot(pool:any){const out:any={};for(const t of tableNames)out[t]=(await pool.query(`select row_to_json(t) as row from ${t} t order by row_to_json(t)::text`)).rows.map((r:any)=>r.row);return out;}
async function capacity(pool:any){return (await pool.query('select scope,active_version,consumed from promotion_campaigns order by scope')).rows;}
try{
 for(const [uid,signal] of [['restore-target','0123456789abcdef'],['restore-control','fedcba9876543210']]){
  const response=(await source.grant(uid,{signal})).response;assert.equal(response.status,200);assert.equal((await response.json()).wallet.credits,7);
 }
 const intent=await source.prepare('restore-guest');
 const response=await source.request('POST','/account/upgrade-intents/complete','restore-target',{requestId:randomUUID(),intentId:intent.intentId,proof:intent.proof,disposition:'PRESERVE_LOCAL_WORK'});assert.equal(response.status,200);
 const before=await snapshot(source.pool);for(const t of tableNames)assert.ok(before[t].length>0,t);
 const campaignBefore=await capacity(source.pool);
 const dbName=(await source.pool.query('select current_database() as name')).rows[0].name;
 const sourceUrl=new URL(adminUrl);sourceUrl.pathname='/'+dbName;
 const pg='/tmp/tmr-pr117-postgres/root/usr/lib/postgresql/18/bin/';
 execFileSync(pg+'pg_dump',['--format=custom','--no-owner','--file='+archive],{env:pgEnv(sourceUrl.toString()),stdio:'pipe'});
 assert.equal(readFileSync(archive).subarray(0,5).toString(),'PGDMP');
 const deleted=await source.request('DELETE','/v1/me','restore-target');assert.equal(deleted.status,202);assert.equal((await deleted.json()).deletion.status,'completed');
 await admin.query('CREATE DATABASE '+restoredName);
 const restoreUrl=new URL(adminUrl);restoreUrl.pathname='/'+restoredName;
 execFileSync(pg+'pg_restore',['--no-owner','--exit-on-error','--single-transaction','--dbname='+restoredName,archive],{env:pgEnv(restoreUrl.toString()),stdio:'pipe'});
 restored=new Pool({connectionString:restoreUrl.toString(),max:2});
 assert.deepEqual(await snapshot(restored),before);
 const config=appConfigSchema.parse({appId:'tailormyresume',firebaseProjectId:'staging-fixture',databaseSecretName:'LOCAL',storageBucketName:'fixture'});
 const database=drizzle(restored);const events:string[]=[];
 const ports={deleteUserFiles:async()=>{events.push('noop-files-fixture')},deleteFirebaseUser:async()=>{events.push('noop-firebase-fixture')}};
 assert.equal(await deleteAccount(database,config,'restore-target',ports),'completed');
 assert.equal(await deleteAccount(database,config,'restore-target',ports),'completed');
 assert.deepEqual(events,['noop-files-fixture','noop-firebase-fixture']);
 const after=await snapshot(restored);
 for(const t of tableNames){assert.equal(after[t].filter((r:any)=>r.user_id==='restore-target'||r.destination_id==='restore-target').length,0,t);assert.deepEqual(after[t].filter((r:any)=>r.user_id==='restore-control'),before[t].filter((r:any)=>r.user_id==='restore-control'));}
 assert.deepEqual(await capacity(restored),campaignBefore);
 await assert.rejects(withLiveUser(database,'restore-target',async()=>null),(e:any)=>e.code==='ACCOUNT_DELETED');
 const report={status:'PASS',kind:'fixture/database integration; real pg_dump/empty restore, candidate deletion service with explicit noop external ports',backendSha:'ba0993ada6055ef61dd000327e0617418755b58b',archiveSha256:createHash('sha256').update(readFileSync(archive)).digest('hex'),archiveBytes:readFileSync(archive).length,migrationCount:(await restored.query('select count(*)::int as count from drizzle.__drizzle_migrations')).rows[0].count,newTableRowsBefore:Object.fromEntries(tableNames.map(t=>[t,before[t].length])),newTableRowsAfterReconciliation:Object.fromEntries(tableNames.map(t=>[t,after[t].length])),checks:['all four new tables nonempty and byte-equivalent JSON rows after empty restore','restored deleted user rows/aliases/proofs/operations removed including destination-bound intent','control account unchanged','capacity consumed unchanged','repeated deletion exactly once external ports','live-user tombstone guard rejects resurrection'],externalAcceptance:'NOT tested; no cloud Firebase/storage action; no live candidate database touched'};
 writeFileSync(output+'/backend-dump-restore-summary.json',JSON.stringify(report,null,2)+'\n');console.log(JSON.stringify(report,null,2));
}finally{if(restored)await restored.end();await admin.query('DROP DATABASE IF EXISTS '+restoredName+' WITH (FORCE)');await admin.end();await source.drop();}
